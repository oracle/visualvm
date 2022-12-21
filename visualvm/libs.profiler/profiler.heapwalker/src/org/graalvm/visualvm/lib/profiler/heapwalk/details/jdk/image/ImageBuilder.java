/*
 * Copyright (c) 2013, 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package org.graalvm.visualvm.lib.profiler.heapwalk.details.jdk.image;

import java.awt.Image;
import java.awt.Point;
import java.awt.color.ColorSpace;
import java.awt.image.BandedSampleModel;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.awt.image.DataBufferUShort;
import java.awt.image.DirectColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.MultiPixelPackedSampleModel;
import java.awt.image.PixelInterleavedSampleModel;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.SinglePixelPackedSampleModel;
import java.awt.image.WritableRaster;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.ImageIcon;
import org.graalvm.visualvm.lib.jfluid.heap.Instance;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.spi.DetailsUtils;

/**
 * Functions to reconstruct image from the heap dump.
 *
 * @author Jan Taus
 */
public class ImageBuilder {

    /** Logger used to log messages related to the image building process. */
    static final Logger LOGGER = Logger.getLogger(ImageDetailProvider.class.getName());


    /**
     * Create image from heap instance.
     *
     * @return <code>null</code> if the image cannot be reconstructed from the heap.
     */
    public static Image buildImage(Instance instance) {
        try {
            return buildImageInternal(instance);
        } catch (FieldAccessor.InvalidFieldException ex) {
            LOGGER.log(Level.FINE, "Unable to create image for instance, error: {0}", ex.getMessage());
            return null;
        }
    }

    static Image buildImageInternal(Instance instance) throws FieldAccessor.InvalidFieldException {
        InstanceBuilder<? extends Image> builder = BUILDERS.getBuilder(instance, Image.class);
        if (builder == null) {
            throw new FieldAccessor.InvalidFieldException("Unable to get Image builder for {0}#{1}", instance.getJavaClass().getName(), instance.getInstanceNumber()); //NOI18N
        }
        return builder.convert(new FieldAccessor(BUILDERS), instance);
    }

    private static final InstanceBuilder<String> TOOKIT_IMAGE_STRING_BUILDER =
            new InstanceBuilder.ReferringInstanceBuilder<>(String.class, "imagerep", "bimage");
    private static final InstanceBuilder<Image> TOOKIT_IMAGE_IMAGE_BUILDER =
            new InstanceBuilder.ReferringInstanceBuilder<>(Image.class, "imagerep", "bimage");
    private static final InstanceBuilder<String> IMAGE_ICON_STRING_BUILDER =
            new InstanceBuilder.ReferringInstanceBuilder<>(String.class, "image");
    private static final InstanceBuilder<Image> IMAGE_ICON_IMAGE_BUILDER =
            new InstanceBuilder.ReferringInstanceBuilder<>(Image.class, "image");
    private static final InstanceBuilder<String> BUFFERED_IMAGE_STRING_BUILDER = new InstanceBuilder<String>(String.class) {
        @Override
        public String convert(FieldAccessor fa, Instance instance) throws FieldAccessor.InvalidFieldException {
            Instance raster = fa.getInstance(instance, "raster", WritableRaster.class, true);   // NOI18N
            int width = fa.getInt(raster, "width");   // NOI18N
            int height = fa.getInt(raster, "height");   // NOI18N
            Instance colorModel = fa.getInstance(instance, "colorModel", ColorModel.class, true);

            int color_count = 0;
            if (FieldAccessor.isInstanceOf(colorModel, IndexColorModel.class)) {
                color_count = DetailsUtils.getIntFieldValue(colorModel, "map_size", 0); // NOI18N
            }
            if (color_count > 0) {
                return Bundle.ImageDetailProvider_ImageDescrColors(width, height, color_count);
            } else {
                return Bundle.ImageDetailProvider_ImageDescr(width, height);
            }
        }
    };
    private static final InstanceBuilder<Image> BUFFERED_IMAGE_IMAGE_BUILDER = new InstanceBuilder<Image>(Image.class) {
        @Override
        public Image convert(FieldAccessor fa, Instance instance) throws FieldAccessor.InvalidFieldException {
            try {
                int imageType = fa.getInt(instance, "imageType"); // NOI18N
                WritableRaster raster = fa.build(instance, "raster", WritableRaster.class, false);
                ColorModel cm = fa.build(instance, "colorModel", ColorModel.class, false);
                BufferedImage result = new BufferedImage(cm, raster, cm.isAlphaPremultiplied(), null);
                result.setData(raster);
                return result;
            } catch (FieldAccessor.InvalidFieldException ex) {
                throw ex;
            } catch (Throwable ex) {
                throw new FieldAccessor.InvalidFieldException("unable to recreate raster: {0}", ex.getMessage()).initCause(ex); // NOI18N
            }
        }
    };
    private static final InstanceBuilder<ColorModel> INDEX_COLOR_MODEL_BUILDER = new InstanceBuilder<ColorModel>(ColorModel.class) {
        @Override
        public ColorModel convert(FieldAccessor fa, Instance instance) throws FieldAccessor.InvalidFieldException {
            int bits = fa.getInt(instance, "pixel_bits"); // NOI18N
            int[] cmap = fa.getIntArray(instance, "rgb", false);// NOI18N
            int size = fa.getInt(instance, "map_size"); // NOI18N
            int trans = fa.getInt(instance, "transparent_index"); // NOI18N
            int transferType = fa.getInt(instance, "transferType"); // NOI18N
            return new IndexColorModel(bits, size, cmap, 0, true, trans, transferType);
        }
    };
    private static final InstanceBuilder<ColorModel> DIRECT_COLOR_MODEL_BUILDER = new InstanceBuilder<ColorModel>(ColorModel.class) {
        @Override
        public ColorModel convert(FieldAccessor fa, Instance instance) throws FieldAccessor.InvalidFieldException {
            int bits = fa.getInt(instance, "pixel_bits"); // NOI18N
            int rmask = fa.getInt(instance, "red_mask"); // NOI18N
            int gmask = fa.getInt(instance, "green_mask"); // NOI18N
            int bmask = fa.getInt(instance, "blue_mask"); // NOI18N
            int amask = fa.getInt(instance, "alpha_mask"); // NOI18N
            boolean ap = fa.getBoolean(instance, "isAlphaPremultiplied"); // NOI18N
            int transferType = fa.getInt(instance, "transferType"); // NOI18N
            return new DirectColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB), bits, rmask, gmask, bmask, amask, ap, transferType);
        }
    };
    private static final InstanceBuilder<ColorModel> COMPONENT_COLOR_MODEL_BUILDER = new InstanceBuilder<ColorModel>(ColorModel.class) {
        @Override
        public ColorModel convert(FieldAccessor fa, Instance instance) throws FieldAccessor.InvalidFieldException {
            int[] bits = fa.getIntArray(instance, "nBits", false);// NOI18N
            int transparency = fa.getInt(instance, "transparency"); // NOI18N
            boolean hasAlpha = fa.getBoolean(instance, "supportsAlpha"); // NOI18N
            boolean ap = fa.getBoolean(instance, "isAlphaPremultiplied"); // NOI18N
            int transferType = fa.getInt(instance, "transferType"); // NOI18N
            return new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB), bits, hasAlpha, ap, transparency, transferType);
        }
    };
    private static final InstanceBuilder<ColorSpace> DEFAULT_COLOR_SPACE_BUILDER = new InstanceBuilder<ColorSpace>(ColorSpace.class) {
        @Override
        public ColorSpace convert(FieldAccessor accessor, Instance instance) throws FieldAccessor.InvalidFieldException {
            return ColorSpace.getInstance(ColorSpace.CS_sRGB);
        }
    };
    private static final InstanceBuilder<WritableRaster> WRITABLE_RASTER_BUILDER = new InstanceBuilder<WritableRaster>(WritableRaster.class) {
        @Override
        public WritableRaster convert(FieldAccessor accessor, Instance instance) throws FieldAccessor.InvalidFieldException {
            WritableRaster parent = accessor.build(instance, "parent", WritableRaster.class, true);
            if (parent == null) {
                DataBuffer dataBuffer = accessor.build(instance, "dataBuffer", DataBuffer.class, false);
                SampleModel sampleModel = accessor.build(instance, "sampleModel", SampleModel.class, false); // NOI18N
                int tx = accessor.getInt(instance, "sampleModelTranslateX");
                int ty = accessor.getInt(instance, "sampleModelTranslateY");
                return Raster.createWritableRaster(sampleModel, dataBuffer, new Point(tx, ty));
            }
            int width = accessor.getInt(instance, "width");
            int height = accessor.getInt(instance, "height");
            int minX = accessor.getInt(instance, "minX");
            int minY = accessor.getInt(instance, "minY");
            int tx = accessor.getInt(instance, "sampleModelTranslateX");
            int ty = accessor.getInt(instance, "sampleModelTranslateY");
            int px = parent.getSampleModelTranslateX();
            int py = parent.getSampleModelTranslateY();
            return parent.createWritableChild(minX, minY, width, height, tx - px + minX, ty - py + minY, null);
        }
    };
    private static final InstanceBuilder<SampleModel> SPP_SAMPLE_MODEL_BUILDER = new InstanceBuilder<SampleModel>(SampleModel.class) {
        @Override
        public SampleModel convert(FieldAccessor fa, Instance instance) throws FieldAccessor.InvalidFieldException {
            int width = fa.getInt(instance, "width");          // NOI18N
            int height = fa.getInt(instance, "height");  // NOI18N
            int dataType = fa.getInt(instance, "dataType");   // NOI18N
            int scanlineStride = fa.getInt(instance, "scanlineStride");  // NOI18N
            int[] bitMasks = fa.getIntArray(instance, "bitMasks", false);  // NOI18N
            return new SinglePixelPackedSampleModel(dataType, width, height, scanlineStride, bitMasks);
        }
    };
    private static final InstanceBuilder<SampleModel> PI_SAMPLE_MODEL_BUILDER = new InstanceBuilder<SampleModel>(SampleModel.class) {
        @Override
        public SampleModel convert(FieldAccessor fa, Instance instance) throws FieldAccessor.InvalidFieldException {
            int width = fa.getInt(instance, "width");          // NOI18N
            int height = fa.getInt(instance, "height");  // NOI18N
            int dataType = fa.getInt(instance, "dataType");   // NOI18N
            int pixelStride = fa.getInt(instance, "pixelStride");  // NOI18N
            int scanlineStride = fa.getInt(instance, "scanlineStride");  // NOI18N
            int[] bandOffsets = fa.getIntArray(instance, "bandOffsets", false);  // NOI18N
            return new PixelInterleavedSampleModel(dataType, width, height, pixelStride, scanlineStride, bandOffsets);
        }
    };
    private static final InstanceBuilder<SampleModel> B_SAMPLE_MODEL_BUILDER = new InstanceBuilder<SampleModel>(SampleModel.class) {
        @Override
        public SampleModel convert(FieldAccessor fa, Instance instance) throws FieldAccessor.InvalidFieldException {
            int width = fa.getInt(instance, "width");          // NOI18N
            int height = fa.getInt(instance, "height");  // NOI18N
            int dataType = fa.getInt(instance, "dataType");   // NOI18N
            int scanlineStride = fa.getInt(instance, "scanlineStride");  // NOI18N
            int[] bankIndices = fa.getIntArray(instance, "bankIndices", false);  // NOI18N
            int[] bandOffsets = fa.getIntArray(instance, "bandOffsets", false);  // NOI18N
            return new BandedSampleModel(dataType, width, height, scanlineStride, bankIndices, bandOffsets);
        }
    };
    private static final InstanceBuilder<SampleModel> MPP_SAMPLE_MODEL_BUILDER = new InstanceBuilder<SampleModel>(SampleModel.class) {
        @Override
        public SampleModel convert(FieldAccessor fa, Instance instance) throws FieldAccessor.InvalidFieldException {
            int width = fa.getInt(instance, "width");          // NOI18N
            int height = fa.getInt(instance, "height");  // NOI18N
            int dataType = fa.getInt(instance, "dataType");   // NOI18N
            int scanlineStride = fa.getInt(instance, "scanlineStride");  // NOI18N
            int numberOfBits = fa.getInt(instance, "numberOfBits");  // NOI18N
            int dataBitOffset = fa.getInt(instance, "dataBitOffset");  // NOI18N
            return new MultiPixelPackedSampleModel(dataType, width, height, numberOfBits, scanlineStride, dataBitOffset);
        }
    };
    private static final InstanceBuilder<DataBuffer> INT_DATA_BUFFER_BUILDER = new InstanceBuilder<DataBuffer>(DataBuffer.class) {
        @Override
        public DataBuffer convert(FieldAccessor fa, Instance instance) throws FieldAccessor.InvalidFieldException {
            int size = fa.getInt(instance, "size");                        // NOI18N
            int[] offsets = fa.getIntArray(instance, "offsets", false);      // NOI18N
            //int[] data = fa.getIntArray(instance, "data", false);     // NOI18N
            int[][] bankdata = fa.getIntArray2(instance, "bankdata", false); // NOI18N
            return new DataBufferInt(bankdata, size, offsets);
        }
    };
    private static final InstanceBuilder<DataBuffer> BYTE_DATA_BUFFER_BUILDER = new InstanceBuilder<DataBuffer>(DataBuffer.class) {
        @Override
        public DataBuffer convert(FieldAccessor fa, Instance instance) throws FieldAccessor.InvalidFieldException {
            int size = fa.getInt(instance, "size");                        // NOI18N
            int[] offsets = fa.getIntArray(instance, "offsets", false);      // NOI18N
            byte[][] bankdata = fa.getByteArray2(instance, "bankdata", false); // NOI18N
            return new DataBufferByte(bankdata, size, offsets);
        }
    };
    private static final InstanceBuilder<DataBuffer> USHORT_DATA_BUFFER_BUILDER = new InstanceBuilder<DataBuffer>(DataBuffer.class) {
        @Override
        public DataBuffer convert(FieldAccessor fa, Instance instance) throws FieldAccessor.InvalidFieldException {
            int size = fa.getInt(instance, "size");                        // NOI18N
            int[] offsets = fa.getIntArray(instance, "offsets", false);      // NOI18N
            short[][] bankdata = fa.getShortArray2(instance, "bankdata", false); // NOI18N
            return new DataBufferUShort(bankdata, size, offsets);
        }
    };

    static final InstanceBuilderRegistry BUILDERS = new InstanceBuilderRegistry();
    static {
        BUILDERS.register(ColorSpace.class, true, DEFAULT_COLOR_SPACE_BUILDER);
        BUILDERS.register(IndexColorModel.class, true, INDEX_COLOR_MODEL_BUILDER);
        BUILDERS.register(ComponentColorModel.class, true, COMPONENT_COLOR_MODEL_BUILDER);
        BUILDERS.register(DirectColorModel.class, true, DIRECT_COLOR_MODEL_BUILDER);
        BUILDERS.register(SinglePixelPackedSampleModel.class, false, SPP_SAMPLE_MODEL_BUILDER);
        BUILDERS.register(PixelInterleavedSampleModel.class, false, PI_SAMPLE_MODEL_BUILDER);
        BUILDERS.register(BandedSampleModel.class, false, B_SAMPLE_MODEL_BUILDER);
        BUILDERS.register(MultiPixelPackedSampleModel.class, false, MPP_SAMPLE_MODEL_BUILDER);
        BUILDERS.register(DataBufferInt.class, false, INT_DATA_BUFFER_BUILDER);
        BUILDERS.register(DataBufferByte.class, false, BYTE_DATA_BUFFER_BUILDER);
        BUILDERS.register(DataBufferUShort.class, false, USHORT_DATA_BUFFER_BUILDER);
        BUILDERS.register(WritableRaster.class, true, WRITABLE_RASTER_BUILDER);
        BUILDERS.register("sun.awt.image.ToolkitImage+", TOOKIT_IMAGE_STRING_BUILDER);
        BUILDERS.register("sun.awt.image.ToolkitImage+", TOOKIT_IMAGE_IMAGE_BUILDER);
        BUILDERS.register(ImageIcon.class, true, IMAGE_ICON_STRING_BUILDER);
        BUILDERS.register(ImageIcon.class, true, IMAGE_ICON_IMAGE_BUILDER);
        BUILDERS.register(BufferedImage.class, true, BUFFERED_IMAGE_STRING_BUILDER);
        BUILDERS.register(BufferedImage.class, true, BUFFERED_IMAGE_IMAGE_BUILDER);
    }
}
