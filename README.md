# AutoMaker
Builder and upload apk to server, then send download link to DingTalk


## Quick Start

Add the plugin to your buildscript:

```groovy
buildscript {
    repositories {
        maven { url 'https://dl.bintray.com/automaker/maven' }
    }

    dependencies {
        classpath 'com.automaker.plugin:automaker-plugin:1.0.1'
    }
}
```

and then apply in your module build.gradle

```groovy
apply plugin: 'automaker-plugin'

buildUploadExtension {
    //打对应buildType的包，如果不配置buildType则默认打release包
    buildType "debug"
    //配置文件服务器地址
    url "http://uploadxxx"
}
sendApkExtension {
    //配置钉钉群机器人access_token，列表格式，支持发送多个群
    access_token = ["access_token1","access_token2"]
    //发送钉钉消息 icon，可以设置为app icon
    picUrl "http://xxx."
}
```

## How to use

Use the task `./gradlew sendApkToDingTalk` to build and send message:

```bash
$ ./gradlew sendApkToDingTalk
```


