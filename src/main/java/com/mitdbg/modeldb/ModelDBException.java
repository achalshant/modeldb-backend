package com.mitdbg.modeldb;

public class ModelDBException extends Exception {

  public ModelDBException() {
    super();
  }

  public ModelDBException(String message) {
    super(message);
  }

  public ModelDBException(String message, Throwable cause) {
    super(message, cause);
  }

  public ModelDBException(Throwable cause) {
    super(cause);
  }

  protected ModelDBException(
      String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
