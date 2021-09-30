package com.benefitj.core.property;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class DefaultPropertiesConverter implements PropertiesConverter {

  private Properties source;

  public DefaultPropertiesConverter(Properties source) {
    this.source = source;
  }

  public DefaultPropertiesConverter(File file) {
    this.source = new Properties();
    try (final FileInputStream fis = new FileInputStream(file)) {
      this.source.load(fis);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  public Properties getSource() {
    return source;
  }

  public void setSource(Properties source) {
    this.source = source;
  }

}
