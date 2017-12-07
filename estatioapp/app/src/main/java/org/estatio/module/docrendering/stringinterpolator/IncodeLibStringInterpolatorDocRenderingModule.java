/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.estatio.module.docrendering.stringinterpolator;

import java.util.Set;

import javax.xml.bind.annotation.XmlRootElement;

import com.google.common.collect.Sets;

import org.apache.isis.applib.Module;
import org.apache.isis.applib.ModuleAbstract;
import org.isisaddons.module.stringinterpolator.IncodeLibStringInterpolatorModule;

import org.incode.module.docrendering.stringinterpolator.dom.StringInterpolatorDocRenderingModule;

import org.estatio.module.document.IncodeDomDocumentModule;

/**
 * This is a "proxy" for the corresponding module defined in the Incode Platform,
 * which we intend to move up into the Estatio codebase
 */
@XmlRootElement(name = "module")
public final class IncodeLibStringInterpolatorDocRenderingModule extends ModuleAbstract {

    @Override
    public Set<Module> getDependencies() {
        return Sets.newHashSet(
                new IncodeDomDocumentModule(),
                new IncodeLibStringInterpolatorModule()
        );
    }

    @Override
    public Set<Class<?>> getDependenciesAsClass() {
        return Sets.newHashSet(StringInterpolatorDocRenderingModule.class);
    }




}
