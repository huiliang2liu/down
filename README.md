## 下载工具
### api说明
#### Down 

| 方法名 | 说明 |
| -- | -- |
| down | 下载 |
| pause | 暂停 |
| isPause | 是否暂停 |
| isDown | 是否在下载 |
| isEnd | 是否下载完成 |
| progress | 下载进度 |



#### DownListener

| 方法名 | 说明 |
| -- | -- |
| downed | 下载完成回调 |
| downFailure | 下载失败回调 |
| onProgressChange | 下载进度变动回调 |


#### InputStreamFactory
| 方法名 | 说明 |
| -- | -- |
| create | 创建下载流 |

####  DownManager
| 方法名 | 说明 |
| -- | -- |
| isDown | 是否下载完 |
| isDowning | 是否正在下载 |
| down | 下载 |
| downURl | 使用URLConnection下载 |
| downOkhttp | 使用Okhttp下载 |