# android-image-weixin-preview
仿微信图片预览的封装，继承AlertDialog，简单易用

Imitate WeChat picture preview, inherit AlertDialog, and it is easy to use

### 效果如图：

The example of picture:

![avatar](https://images.ylwx365.com/images/mini/63221619318502001.jpg)


### 安装：

Install：

导入CustomPreview.java和CustomPreviewAdapter.java到项目中即可

Import CustomPreview.java and CustomPreviewAdapter.java to your project

### 使用方法：

How to use：

法一：

one：
```java
new CustomPreview(QualificationActivity.this, imgArr).show();
```
imgArr是图片链接集合List<String>，效果是渐进渐出
  
imgArr is List<String>，the effect is fadein or fadeout
  
法二：

two：
```java
new CustomPreview(QualificationActivity.this, imgArr, simpleDraweeViewList).show();
```
simpleDraweeViewList是图片对象集合List<SimpleDraweeView>，效果是仿微信从图片位置放大效果和缩小到恢复位置
  
simpleDraweeViewList is List<SimpleDraweeView>，the effect is of imitating WeChat zoom in or zoom out from the initial postion
  
### 其他方法：

Other methods：
设置图片描述

Set picture title
```java
setTitleList(List<String> titleList)
```

设置当前第几个图片

Set picture current position
```java
setCurrentPosition(Integer currentPosition)
```

## 欢迎打赏
一分钱也是源源不断技术的推动力

Chinese friends welcome to reward, and foreign friends welcome to give me a star

<img src="https://images.ylwx365.com/images/mini/14911619318881657.jpg" alt="图片加载中.." width="200" />

