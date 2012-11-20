@rem DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
@rem
@rem Copyright 1997-2010 Oracle and/or its affiliates. All rights reserved.
@rem
@rem Oracle and Java are registered trademarks of Oracle and/or its affiliates.
@rem Other names may be trademarks of their respective owners.
@rem
@rem The contents of this file are subject to the terms of either the GNU
@rem General Public License Version 2 only ("GPL") or the Common
@rem Development and Distribution License("CDDL") (collectively, the
@rem "License"). You may not use this file except in compliance with the
@rem License. You can obtain a copy of the License at
@rem http://www.netbeans.org/cddl-gplv2.html
@rem or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
@rem specific language governing permissions and limitations under the
@rem License.  When distributing the software, include this License Header
@rem Notice in each file and include the License file at
@rem nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
@rem particular file as subject to the "Classpath" exception as provided
@rem by Oracle in the GPL Version 2 section of the License file that
@rem accompanied this code. If applicable, add the following below the
@rem License Header, with the fields enclosed by brackets [] replaced by
@rem your own identifying information:
@rem "Portions Copyrighted [year] [name of copyright owner]"
@rem
@rem Contributor(s):
@rem
@rem The Original Software is NetBeans. The Initial Developer of the Original
@rem Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
@rem Microsystems, Inc. All Rights Reserved.
@rem
@rem If you wish your version of this file to be governed by only the CDDL
@rem or only the GPL Version 2, indicate your decision by adding
@rem "[Contributor] elects to include this software in this distribution
@rem under the [CDDL or GPL Version 2] license." If you do not indicate a
@rem single choice of license, a recipient has the option to distribute
@rem your version of this file under either the CDDL, the GPL Version 2 or
@rem to extend the choice of license to its licensees as provided above.
@rem However, if you add GPL Version 2 code and therefore, elected the GPL
@rem Version 2 license, then the option applies only if the new code is
@rem made subject to such option by the copyright holder.

@echo off

REM This script expects that NetBeans IDE runs under JDK 5.0 update 4 (or later) by default.
REM If you need to specify the JDK explicitely, you can force the IDE to start with specific JDK
REM by passing -jdkhome <path> parameter to nb.exe/netbeans.exe e.g. "nb.exe -jdkhome C:\Software\jdk15_04"

nb.exe -J-agentpath:"%~dp0\..\profiler\lib\deployed\jdk15\windows\profilerinterface.dll"="\"%~dp0\..\profiler\lib\"",5140 %*