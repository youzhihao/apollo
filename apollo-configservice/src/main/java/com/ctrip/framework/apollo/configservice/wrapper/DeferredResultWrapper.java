package com.ctrip.framework.apollo.configservice.wrapper;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import com.ctrip.framework.apollo.core.dto.ApolloConfigNotification;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.List;
import java.util.Map;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
public class DeferredResultWrapper {
  private static final long TIMEOUT = 60 * 1000;//60 seconds
  private static final ResponseEntity<List<ApolloConfigNotification>>
      NOT_MODIFIED_RESPONSE_LIST = new ResponseEntity<>(HttpStatus.NOT_MODIFIED);

  //原始的namespace和处理过后的namespace的一个映射关系
  private Map<String, String> normalizedNamespaceNameToOriginalNamespaceName;
  //核心类，http请求hold住60秒的关键类
  private DeferredResult<ResponseEntity<List<ApolloConfigNotification>>> result;


  public DeferredResultWrapper() {
    result = new DeferredResult<>(TIMEOUT, NOT_MODIFIED_RESPONSE_LIST);
  }

  public void recordNamespaceNameNormalizedResult(String originalNamespaceName, String normalizedNamespaceName) {
    if (normalizedNamespaceNameToOriginalNamespaceName == null) {
      normalizedNamespaceNameToOriginalNamespaceName = Maps.newHashMap();
    }
    normalizedNamespaceNameToOriginalNamespaceName.put(normalizedNamespaceName, originalNamespaceName);
  }


  public void onTimeout(Runnable timeoutCallback) {
    result.onTimeout(timeoutCallback);
  }

  public void onCompletion(Runnable completionCallback) {
    result.onCompletion(completionCallback);
  }


  public void setResult(ApolloConfigNotification notification) {
    setResult(Lists.newArrayList(notification));
  }

  /**
   * The namespace name is used as a key in client side, so we have to return the original one instead of the correct one
   * 这里
   */
  public void setResult(List<ApolloConfigNotification> notifications) {
    //这里做一个转换，再将处理过后的标准namespace转换成客户端传过来的namespace
    if (normalizedNamespaceNameToOriginalNamespaceName != null) {
      notifications.stream().filter(notification -> normalizedNamespaceNameToOriginalNamespaceName.containsKey
          (notification.getNamespaceName())).forEach(notification -> notification.setNamespaceName(
              normalizedNamespaceNameToOriginalNamespaceName.get(notification.getNamespaceName())));
    }

    result.setResult(new ResponseEntity<>(notifications, HttpStatus.OK));
  }

  public DeferredResult<ResponseEntity<List<ApolloConfigNotification>>> getResult() {
    return result;
  }
}
