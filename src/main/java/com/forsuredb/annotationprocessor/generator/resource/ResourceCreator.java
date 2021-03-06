/*
   forsuredbcompiler, an annotation processor and code generator for the forsuredb project

   Copyright 2015 Ryan Scott

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package com.forsuredb.annotationprocessor.generator.resource;

import com.forsuredb.annotationprocessor.util.APLog;

import java.io.IOException;

import javax.annotation.processing.ProcessingEnvironment;
import javax.tools.FileObject;
import javax.tools.JavaFileManager;
import javax.tools.StandardLocation;

/**
 * <p>
 *     A helper that makes the creation of new resources easier.
 * </p>
 * @author Ryan Scott
 */
/*package*/ class ResourceCreator {

    private static final String LOG_TAG = ResourceCreator.class.getSimpleName();

    private final JavaFileManager.Location location;
    private final CharSequence pkg;
    private final CharSequence relativeName;

    public ResourceCreator(final String relativeName) {
        this(System.getProperty("applicationPackageName"), relativeName, StandardLocation.CLASS_OUTPUT);
    }

    public ResourceCreator(String pkg, String relativeName, JavaFileManager.Location location) {
        this.location = location;
        this.pkg = pkg;
        this.relativeName = relativeName;
    }

    public FileObject create(ProcessingEnvironment processingEnv) throws IOException {
        APLog.i(LOG_TAG, "creating resource at location: " + location.getName());
        return processingEnv.getFiler().createResource(location, pkg, relativeName);
    }
}
