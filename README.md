**v1.1.3版本更新说明:**

1.支持Max的自渲染广告接入，目前能适配Applovin，Admob的接入

2.自渲染广告必须将相关的View设置进入ATNativePrepareInfo里面，以下是必须设置的：

| API                                  | 说明              |
| ------------------------------------ | ----------------- |
| setTitleView(View titleView)         | 绑定标题View      |
| setIconView(View iconView)           | 绑定应用图标 View |
| setMainImageView(View mainImageView) | 绑定大图View      |
| setDescView(View descView)           | 绑定描述View      |
| setCtaView(View ctaView)             | 绑定CTA按钮View   |



# 集成

Tip: If necessary, please refer to [the English documentation](https://github.com/Alex-only/AlexMaxDemo_Android/blob/main/README_EN.md)

## 一. 接入TopOn SDK

请参考[TopOn SDK集成文档](https://docs.toponad.com/#/zh-cn/android/android_doc/android_sdk_config_access)接入TopOn SDK，建议接入**TopOn v6.1.65及以上版本**



## 二. 引入Max SDK&Alex Adapter

### Android

#### 1. 引入Max SDK

在build.gradle中添加以下代码，引入平台SDK

```java
dependencies {
    //Max SDK
	api 'com.applovin:applovin-sdk:11.9.0'
}
```

#### 2. 引入Alex Adapter

**注意**：以下方式任选其一即可

2.1 Gradle引入(推荐)：

在build.gradle中添加以下代码：

```java
repositories {
    mavenCentral()
}

dependencies {
    //Alex Adapter
    api 'io.github.alex-only:max_adapter:1.1.3'
}
```

2.2 aar：

将alex_adapter_max.aar放到项目module的libs文件夹下（如果没有libs文件夹，则需要创建），然后在build.gradle中进行引入

```java
dependencies {
    implementation fileTree(dir: 'libs', include: ['*.jar','*.aar'])
}
```

2.3 源码：

*将AlexLib/src/main/java目录下的代码复制拷贝到项目module下的src/main/java中，可根据需要修改各个Adapter的包名或者类名。

*在项目的proguard-rules.pro中添加以下混淆规则（如果有修改类名，keep的类名需改为修改后的类名）

```java
-keep class com.alex.** { *;}
-keepclassmembers public class com.alex.** {
   public *;
}
```



### Unity

在 Assets/AnyThinkAds/Plugins/Android/NonChina/mediation目录下添加文件：`Max/Editor/Dependencies.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<dependencies>
    <androidPackages>

        <androidPackage spec="com.applovin:applovin-sdk:11.9.0"/>
        <androidPackage spec="io.github.alex-only:max_adapter:1.1.3"/>
        
    </androidPackages>
</dependencies>
```



### 3. Adapter中使用的Key说明

```
"sdk_key": 广告平台的SDK Key
"unit_id": 广告平台的广告位ID
"unit_type": 广告位类型，0: Banner, 1: MREC
```

后台添加广告源时的JSON配置示例如下：（xxx需要替换为Max实际的SDK key以及广告位ID，非横幅广告位不需要配置"unit_type"）

```json
{
    "sdk_key":"xxx",
    "unit_id":"xxx",
    "unit_type":"0"
}
```



## 三. Max接入其他广告平台

如果不需要通过Max接入其他广告平台，可跳过此部分内容。以接入Mintegral为例：

1、先到 [TopOn后台](https://docs.toponad.com/#/zh-cn/android/download/package)，查看接入的TopOn版本兼容的Mintegral版本是多少？（TopOn v6.1.65版本兼容的Mintegral版本为v16.3.61）

2、然后到 [Max后台](https://dash.applovin.com/documentation/mediation/android/mediation-adapters#adapter-network-information)，根据接入的Max SDK版本（v11.6.0）和Mintegral版本（v16.3.61），查找对应的Adapter版本（即v16.3.61.0）

**注意：**

（1）如果找不到Mintegral v16.3.61版本对应的Adapter，可通过查看Adapter的Changelog，找到对应的Adapter版本

（2）需确保TopOn和Max都兼容Mintegral SDK

![img](img/image4.png)

3、引入Gradle依赖：

```java
dependencies {
    implementation 'com.applovin.mediation:mintegral-adapter:16.3.61.0'
}
```



## 四. TopOn后台配置

1、按照SDK对接文档接入同时，需要在后台添加自定义广告平台

![img](img/image1.png)

2、选择【自定义广告平台】，填写广告平台名称、账号名称，按照SDK的对接文档填写Adapter

*广告平台名称需要写上Max，便于区分广告平台，建议名称格式：Max_XXXXX

![img](img/image2.png)

**注意**：如果是使用gradle、aar方式或者直接使用源码方式（没有修改类名），请配置以下类名。如果修改了类名，请配置修改后的类名

```
激励视频：com.alex.AlexMaxRewardedVideoAdapter
插屏：com.alex.AlexMaxInterstitialAdapter
横幅：com.alex.AlexMaxBannerAdapter
原生：com.alex.AlexMaxNativeAdapter
开屏：com.alex.AlexMaxSplashAdapter
```

![img](img/image5.png)

3、记录广告平台ID

![img](img/image3.png)

以上配置都完成之后，可以添加广告源配置



## 五. Max后台配置

### Step1.创建MAX帐号

登录[MAX官网](https://dash.applovin.com/o/mediation)申请开通账号



### Step2.创建MAX的应用和广告单元

在MAX-->Manage-->Ad Units中创建应用和广告位

![](img/max_1.png)



### Step3.在MAX完成Network信息配置

![](img/max_2.png)



### Step4. MAX广告位说明

MAX的Unit跟TopOn的广告类型对应关系如下：

| MAX-Unit     | TopOn-广告类型              |
| ------------ | --------------------------- |
| Banner       | 横幅广告 Banner             |
| Interstitial | 插屏广告 Interstitial       |
| Rewarded     | 激励视频广告 Rewarded Video |
| App Open     | 开屏广告 Splash             |
| Native       | 原生广告 Native             |



### Step5. 在后台配置MAX广告位

#### 5.1 配置MAX 的广告源

5.1.1 通过以下路径获取MAX 的Ad Unit ID：MAX-->Manage-->Ad Units

![](img/max_3.png)



5.1.2. 将MAX的参数配置在TopOn后台

添加广告源，登录TopOn后台→广告平台→变现平台→广告源管理（Max）→添加广告源

![](img/max_4.png)



