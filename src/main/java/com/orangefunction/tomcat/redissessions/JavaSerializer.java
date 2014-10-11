package com.orangefunction.tomcat.redissessions;

import org.apache.catalina.util.CustomObjectInputStream;

import javax.servlet.http.HttpSession;

import java.util.Enumeration;
import java.util.HashMap;
import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;

public class JavaSerializer implements Serializer {
  private ClassLoader loader;

  private final Log log = LogFactory.getLog(JavaSerializer.class);

  @Override
  public void setClassLoader(ClassLoader loader) {
    this.loader = loader;
  }

  public byte[] attributesHashFrom(RedisSession session) throws IOException {
    HashMap<String,Object> attributes = new HashMap<String,Object>();
    for (Enumeration<String> enumerator = session.getAttributeNames(); enumerator.hasMoreElements();) {
      String key = enumerator.nextElement();
      attributes.put(key, session.getAttribute(key));
    }

    byte[] serialized = null;
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(bos));
    try {
      oos.writeUnshared(attributes);
      oos.flush();
      serialized = bos.toByteArray();
    } finally {
      oos.close();
      bos.close();
    }

    MessageDigest digester = null;
    try {
      digester = MessageDigest.getInstance("MD5");
    } catch (NoSuchAlgorithmException e) {
      log.error("Unable to get MessageDigest instance for MD5");
    }
    return digester.digest(serialized);
  }

  @Override
  public byte[] serializeFrom(RedisSession session, SessionSerializationMetadata metadata) throws IOException {
    byte[] serialized = null;
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(bos));
    try {
      oos.writeObject(metadata);
      session.writeObjectData(oos);
      oos.flush();
      serialized = bos.toByteArray();
    } finally {
      oos.close();
      bos.close();
    }

    return serialized;
  }

  @Override
  public void deserializeInto(byte[] data, RedisSession session, SessionSerializationMetadata metadata) throws IOException, ClassNotFoundException {
    BufferedInputStream bis = new BufferedInputStream(new ByteArrayInputStream(data));
    ObjectInputStream ois = new CustomObjectInputStream(bis, loader);
    try {
      SessionSerializationMetadata serializedMetadata = (SessionSerializationMetadata)ois.readObject();
      metadata.copyFieldsFrom(serializedMetadata);
      session.readObjectData(ois);
    } finally {
      ois.close();
      bis.close();
    }
  }
}
