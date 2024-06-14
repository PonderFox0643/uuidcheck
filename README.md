# uuidcheck

**是时候该宣布uuidcheck退役了，其实猫踢螺钉已经可以满足ygg的所有要求**

**我承认做此插件的时候不认识 猫踢螺钉 （https://github.com/CaaMoe/MultiLogin）**

**自此宣布uuidcheck正式退役**


这是简单粗暴的解决uuid不同玩家名相同的情况
解决符合以下条件的服务器：
正版第三方认证共存
使用了YggdrasilOfficialProxy
防止同名不同uuid的插件就来了

使用方法：
安装到服务端后它会自动生成config文件在plugins/uuidcheck
在里面填上你的MySQL信息并保存，重启服务端




思路如下：
此插件的数据库为MySQL，读取config.yml以获取MySQL连接方式包括主机名、端口、用户名、密码、数据库名。插件运行时生成对应的数据库表。当玩家进入服务器时按照以下方式判断，如果数据库中存在这个玩家的名字并且uuid相符时他将可以进入，如果uuid不一样，名字一样时将阻止这个玩家登录并提示“登录方式不对：这里出现了与你名字一样但uuid不同的人”，如果有uuid相同玩家名对应不上数据库的情况则将他的名字覆盖到对应此uuid数据表玩家名上



由chatGPT编写
