# android-image-weixin-preview
仿微信图片预览的封装，继承AlertDialog，简单易用

使用方法：
法一：
new CustomPreview(QualificationActivity.this, imgArr).show();
imgArr是图片链接集合List<String>，效果是渐进渐出
  
法二：
new CustomPreview(QualificationActivity.this, imgArr, simpleDraweeViewList).show();
simpleDraweeViewList是图片对象集合List<SimpleDraweeView>，效果是仿微信从图片位置放大效果和缩小到恢复位置
  
其他方法：
设置图片描述
setTitleList(List<String> titleList)

设置当前第几个图片
setCurrentPosition(Integer currentPosition)

