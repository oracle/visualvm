/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2013 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 *
 * Contributor(s):
 *
 * Portions Copyrighted 2013 Sun Microsystems, Inc.
 */
package org.netbeans.modules.profiler.heapwalk.details.jdk.image;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BandedSampleModel;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.awt.image.IndexColorModel;
import java.awt.image.MultiPixelPackedSampleModel;
import java.awt.image.PixelInterleavedSampleModel;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.SinglePixelPackedSampleModel;
import java.awt.image.WritableRaster;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.modules.profiler.heapwalk.details.spi.DetailsProvider;
import org.netbeans.modules.profiler.heapwalk.details.spi.DetailsUtils;
import org.netbeans.modules.profiler.heapwalk.details.jdk.image.FieldAccessor.InvalidFieldException;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jan Taus
 */
@NbBundle.Messages({
    "ImageDetailProvider_ImageDescr={0}x{1}", // NOI18N
    "ImageDetailProvider_ImageDescrColors=size={0}x{1}, {2} colors" // NOI18N
})
@ServiceProvider(service = DetailsProvider.class)
public class ImageDetailProvider extends DetailsProvider.Basic {

    private static final Logger LOGGER = Logger.getLogger(ImageDetailProvider.class.getName());
    private static final String BROKEN_IMAGE_NAME = "org/netbeans/modules/profiler/heapwalk/details/jdk/image/broken-image.png"; //NOI18N
    private static final int CHECKER_SIZE = 8;
    private static final Color CHECKER_BG = Color.LIGHT_GRAY;
    private static final Color CHECKER_FG = Color.DARK_GRAY;
    private static final InstanceBuilderRegistry builders = new InstanceBuilderRegistry();

    public ImageDetailProvider() {
        super(SUPPORTED_CLASSES);
    }

    @Override
    public String getDetailsString(String className, Instance instance, Heap heap) {
        try {
            InstanceBuilder<? extends String> builder = builders.getBuilder(instance, String.class);
            if (builder == null) {
                LOGGER.log(Level.FINE, "Unable to get String builder for %s", className);
            } else {
                return builder.convert(new FieldAccessor(heap, builders), instance);
            }
        } catch (InvalidFieldException ex) {
            LOGGER.log(Level.FINE, "Unable to get text for instance", ex.getMessage());
        }
        return null;
    }

    @Override
    public View getDetailsView(String className, Instance instance, Heap heap) {
        return new ImageView(instance, heap);
    }

    private static class ImageView extends DetailsProvider.View {

        public ImageView(Instance instance, Heap heap) {
            super(instance, heap);
        }

        @Override
        protected void computeView(Instance instance, Heap heap) {
            FieldAccessor fa = new FieldAccessor(heap, builders);
            JLabel label;
            try {
                Image image = buildImageInternal(instance, heap);

                int width = image.getWidth(null);
                int height = image.getHeight(null);
                BufferedImage background = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
                Graphics2D g = background.createGraphics();
                drawChecker(g, width, height);
                g.drawImage(image, 0, 0, null);

                label = new JLabel(new ImageIcon(background));
            } catch (InvalidFieldException ex) {
                LOGGER.log(Level.FINE, "Unable to get text for instance", ex.getMessage());
                label = new JLabel(ImageUtilities.loadImageIcon(BROKEN_IMAGE_NAME, false));
                if(LOGGER.isLoggable(Level.FINE)) {
                    label.setToolTipText(ex.getMessage()); //TODO: unlocalized message exposed, only in debug mode
                }
            }
            if (label != null) {
                final JComponent component = label;
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        removeAll();
                        add(component, BorderLayout.CENTER);
                        revalidate();
                        doLayout();
                        repaint();
                    }
                });
            }
        }

        private static void drawChecker(Graphics2D g, int width, int height) {
            g.setColor(CHECKER_BG);
            g.fillRect(0, 0, width, height);
            g.setColor(CHECKER_FG);
            for (int i = 0; i < (width + CHECKER_SIZE - 1) / CHECKER_SIZE; i++) {
                for (int j = 0; j < (height + CHECKER_SIZE - 1) / CHECKER_SIZE; j++) {
                    if ((i + j) % 2 == 0) {
                        g.fillRect(i * CHECKER_SIZE, j * CHECKER_SIZE, CHECKER_SIZE, CHECKER_SIZE);
                    }
                }

            }
        }
    }

    private static Image buildImageInternal(Instance instance, Heap heap) throws InvalidFieldException {

        InstanceBuilder<? extends Image> builder = builders.getBuilder(instance, Image.class);
        if (builder == null) {
            return null;
        }
        return builder.convert(new FieldAccessor(heap, builders), instance);

    }

    /**
     * Create image from heap instance.
     *
     * @return <code>null</code> is the image cannot be reconstructed from the heap.
     */
    public static Image buildImage(Instance instance, Heap heap) {
        try {
            return buildImageInternal(instance, heap);
        } catch (InvalidFieldException ex) {
            LOGGER.log(Level.FINE, "Unable to create image for instance", ex.getMessage());
            return null;
        }
    }
    private static final InstanceBuilder<String> TOOKIT_IMAGE_STRING_BUILDER =
            new InstanceBuilder.ReferringInstanceBuilder<String>(String.class, "imagerep", "bimage");
    private static final InstanceBuilder<Image> TOOKIT_IMAGE_IMAGE_BUILDER =
            new InstanceBuilder.ReferringInstanceBuilder<Image>(Image.class, "imagerep", "bimage");
    private static final InstanceBuilder<String> IMAGE_ICON_STRING_BUILDER =
            new InstanceBuilder.ReferringInstanceBuilder<String>(String.class, "image");
    private static final InstanceBuilder<Image> IMAGE_ICON_IMAGE_BUILDER =
            new InstanceBuilder.ReferringInstanceBuilder<Image>(Image.class, "image");
    private static final InstanceBuilder<String> BUFFERED_IMAGE_STRING_BUILDER = new InstanceBuilder<String>(String.class) {
        @Override
        public String convert(FieldAccessor fa, Instance instance) throws InvalidFieldException {
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
        public Image convert(FieldAccessor fa, Instance instance) throws InvalidFieldException {
            try {
                int imageType = fa.getInt(instance, "imageType"); // NOI18N
                WritableRaster raster = fa.build(instance, "raster", WritableRaster.class, false);
                Instance colorModel = fa.getInstance(instance, "colorModel", ColorModel.class, true);
                BufferedImage result;
                if (FieldAccessor.isInstanceOf(colorModel, IndexColorModel.class)) {
                    IndexColorModel indexColorModel = buildIndexColorModel(fa, colorModel);
                    result = new BufferedImage(raster.getWidth(), raster.getHeight(), imageType, indexColorModel);
                } else {
                    //we hope the same color model will be constructed for the same image type
                    result = new BufferedImage(raster.getWidth(), raster.getHeight(), imageType);
                }
                result.setData(raster);
                return result;
            } catch (InvalidFieldException ex) {
                throw ex;
            } catch (Throwable ex) {
                throw new InvalidFieldException("unable to recreate raster: %s", ex.getMessage()).initCause(ex); // NOI18N
            }
        }
    };

    private static IndexColorModel buildIndexColorModel(FieldAccessor fa, Instance instance) throws InvalidFieldException {
        int bits = fa.getInt(instance, "pixel_bits"); // NOI18N
        int[] cmap = fa.getIntArray(instance, "rgb", false);// NOI18N
        int size = fa.getInt(instance, "map_size"); // NOI18N
        int trans = fa.getInt(instance, "transparent_index"); // NOI18N
        int transferType = fa.getInt(instance, "transferType"); // NOI18N
        return new IndexColorModel(bits, size, cmap, 0, true, trans, transferType);
    }
    private static final InstanceBuilder<WritableRaster> WRITABLE_RASTER_BUILDER = new InstanceBuilder<WritableRaster>(WritableRaster.class) {
        @Override
        public WritableRaster convert(FieldAccessor accessor, Instance instance) throws InvalidFieldException {
            DataBuffer dataBuffer = accessor.build(instance, "dataBuffer", DataBuffer.class, false);
            SampleModel sampleModel = accessor.build(instance, "sampleModel", SampleModel.class, false); // NOI18N
            return Raster.createWritableRaster(sampleModel, dataBuffer, null);
        }
    };
    private static final InstanceBuilder<SampleModel> SPP_SAMPLE_MODEL_BUILDER = new InstanceBuilder<SampleModel>(SampleModel.class) {
        @Override
        public SampleModel convert(FieldAccessor fa, Instance instance) throws InvalidFieldException {
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
        public SampleModel convert(FieldAccessor fa, Instance instance) throws InvalidFieldException {
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
        public SampleModel convert(FieldAccessor fa, Instance instance) throws InvalidFieldException {
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
        public SampleModel convert(FieldAccessor fa, Instance instance) throws InvalidFieldException {
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
        public DataBuffer convert(FieldAccessor fa, Instance instance) throws InvalidFieldException {
            int size = fa.getInt(instance, "size");                        // NOI18N
            int[] offsets = fa.getIntArray(instance, "offsets", false);      // NOI18N
            int[] data = fa.getIntArray(instance, "data", false);     // NOI18N
            int[][] bankdata = fa.getIntArray2(instance, "bankdata", false); // NOI18N
            return new DataBufferInt(bankdata, size, offsets);
        }
    };
    private static final InstanceBuilder<DataBuffer> BYTE_DATA_BUFFER_BUILDER = new InstanceBuilder<DataBuffer>(DataBuffer.class) {
        @Override
        public DataBuffer convert(FieldAccessor fa, Instance instance) throws InvalidFieldException {
            int size = fa.getInt(instance, "size");                        // NOI18N
            int[] offsets = fa.getIntArray(instance, "offsets", false);      // NOI18N
            byte[] data = fa.getByteArray(instance, "data", false);  // NOI18N
            byte[][] bankdata = fa.getByteArray2(instance, "bankdata", false); // NOI18N
            return new DataBufferByte(bankdata, size, offsets);

        }
    };

    static {
        builders.register(SinglePixelPackedSampleModel.class, false, SPP_SAMPLE_MODEL_BUILDER);
        builders.register(PixelInterleavedSampleModel.class, false, PI_SAMPLE_MODEL_BUILDER);
        builders.register(BandedSampleModel.class, false, B_SAMPLE_MODEL_BUILDER);
        builders.register(MultiPixelPackedSampleModel.class, false, MPP_SAMPLE_MODEL_BUILDER);
        builders.register(DataBufferInt.class, false, INT_DATA_BUFFER_BUILDER);
        builders.register(DataBufferByte.class, false, BYTE_DATA_BUFFER_BUILDER);
        builders.register(WritableRaster.class, true, WRITABLE_RASTER_BUILDER);
        builders.register("sun.awt.image.ToolkitImage+", TOOKIT_IMAGE_STRING_BUILDER);
        builders.register("sun.awt.image.ToolkitImage+", TOOKIT_IMAGE_IMAGE_BUILDER);
        builders.register(ImageIcon.class, false, IMAGE_ICON_STRING_BUILDER);
        builders.register(ImageIcon.class, false, IMAGE_ICON_IMAGE_BUILDER);
        builders.register(BufferedImage.class, true, BUFFERED_IMAGE_STRING_BUILDER);
        builders.register(BufferedImage.class, true, BUFFERED_IMAGE_IMAGE_BUILDER);
    }
    private static final String[] SUPPORTED_CLASSES = builders.getMasks(Image.class, String.class);
}
