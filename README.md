# NaiveReader | 简单的小说阅读器
主要功能
1. 网络小说追更
   * 目前支持n个网站
   * 搜索小说，自动下载，增量更新
2. txt阅读
   * 自动分章节方便定位（过程中使用了正则表达式搜索关键字作为章节锚点，不一定好用，凑合用）
3. 自定义打包格式，主要是配合工具 [bbsreader](https://github.com/jackfnx/bbsreader) 使用
   * [bbsreader](https://github.com/jackfnx/bbsreader) 是一个Windows程序，是本程序的扩展，本程序通过局域网与 [bbsreader](https://github.com/jackfnx/bbsreader) 连接，从其中下载打包文件
   * [bbsreader](https://github.com/jackfnx/bbsreader) 提供的打包文件包括几类
     * 论坛（目前主要支持恶魔岛）下载的帖子主题及其汇总（具体的分类也很多，详情见 [bbsreader](https://github.com/jackfnx/bbsreader) 的介绍）
     * 通过将txt手动打包得到的更方便阅读的包，这是因为
       * 可以手动调节正则表达式，分章节更准确
       * 可以将多个txt打包到一起
