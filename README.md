# 集成

## 一. 接入TopOn SDK

请参考[TopOn SDK集成文档](https://docs.toponad.com/#/zh-cn/android/android_doc/android_sdk_config_access)接入TopOn SDK，建议接入**TopOn v6.1.65及以上版本**



## 二. 引入Alex Adapter

1、在build.gradle中添加以下代码，引入平台SDK

```java
dependencies {
    api 'com.applovin:applovin-sdk:11.6.0'
}
```

2、将AlexLib/src/main/java目录下的代码复制拷贝到项目module下的src/main/java中，可根据需要修改各个Adapter的包名或者类名

3、在项目的proguard-rules.pro中添加以下混淆规则（如果有修改类名，keep的类名需改为修改后的类名）

```java
-keep class com.alex.** { *;}
-keepclassmembers public class com.alex.** {
   public *;
}
```

4、Adapter中使用的Key说明如下：

```
"sdk_key": 广告平台的SDK Key
"unit_id": 广告平台的广告位ID
"unit_type": 广告位类型，0: Banner, 1: MREC
```

后台添加广告源时的JSON配置示例如下：（xxx需要替换为Max实际的SDK key以及广告位ID，非横幅广告位不需要配置"unit_type"）

```
{
    "sdk_key":"xxx",
    "unit_id":"xxx",
    "unit_type":"0"
}
```



### 三. 后台配置

1、按照SDK对接文档接入同时，需要在后台添加自定义广告平台

![img](img/image1.png)

2、选择【自定义广告平台】，填写广告平台名称、账号名称，按照SDK的对接文档填写Adapter

*广告平台名称需要写上Max，便于区分广告平台，建议名称格式：Max_XXXXX

![img](img/image2.png)

3、记录广告平台ID

![img](img/image3.png)

4、广告平台添加完成后，再添加广告源（添加广告源时按照对应样式配置即可）

5、可编辑广告平台设置，选择是否开通报表api并拉取数据





