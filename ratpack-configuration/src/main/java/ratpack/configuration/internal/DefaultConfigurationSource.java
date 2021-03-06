/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ratpack.configuration.internal;

import com.google.common.io.ByteSource;
import com.google.common.io.Files;
import com.google.common.io.Resources;
import ratpack.configuration.ConfigurationSource;

import java.io.File;
import java.net.URL;
import java.util.Properties;

public class DefaultConfigurationSource implements ConfigurationSource {
  private final ClassLoader classLoader;
  private final Properties overrideProperties;
  private final Properties defaultProperties;
  private final ByteSource byteSource;

  public DefaultConfigurationSource(ClassLoader classLoader, ByteSource byteSource, Properties overrideProperties, Properties defaultProperties) {
    this.classLoader = classLoader;
    this.byteSource = byteSource;
    this.overrideProperties = overrideProperties;
    this.defaultProperties = defaultProperties;
  }

  public DefaultConfigurationSource(ClassLoader classLoader, Properties overrideProperties, Properties defaultProperties) {
    this(classLoader, ByteSource.empty(), overrideProperties, defaultProperties);
  }

  public DefaultConfigurationSource(ClassLoader classLoader, File file, Properties overrideProperties, Properties defaultProperties) {
    this(classLoader, Files.asByteSource(file), overrideProperties, defaultProperties);
  }

  public DefaultConfigurationSource(ClassLoader classLoader, URL url, Properties overrideProperties, Properties defaultProperties) {
    this(classLoader, Resources.asByteSource(url), overrideProperties, defaultProperties);
  }

  @Override
  public ClassLoader getClassLoader() {
    return classLoader;
  }

  @Override
  public ByteSource getByteSource() {
    return byteSource;
  }

  @Override
  public Properties getOverrideProperties() {
    return overrideProperties;
  }

  @Override
  public Properties getDefaultProperties() {
    return defaultProperties;
  }
}
