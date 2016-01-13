
##代码拷贝与二次合并##

###创建分支
* 其实主线和分支只是一种称呼，本质就是两个路径不同的文件夹。
* 创建分支，就是复制其他文件到一个新的目录里继续开发。
```
cd www/lib/
svn copy svn://wyx@svn.haoxitech.com/haoframe/web/HaoConnect/php/HaoConnect .
svn ci . -m "copy HaoConnect first time"
```

###在分支上继续开发
*其实就是拷贝出文件后，在新文件里，继续开工


###合并预览
* 当你在分支里开工一段时间后，你可以比较两个文件夹的变化（其实你可以比较任何文件并合并他们，只是这里用于了主线和分支这两个特定的场景中而已）
* --dry-run选项是只提供合并后状态的预览，并不实际更改文件
```
cd www/lib/HaoConnect
svn merge svn://wyx@svn.haoxitech.com/haoframe/web/HaoConnect/php/HaoConnect --dry-run
```


###直接合并
```
cd www/lib/HaoConnect
svn merge svn://svn.haoxitech.com/haoframe/web/HaoConnect/php/HaoConnect
```
* 如果合并是出现冲突，可以选择p来延迟，然后针对性的编辑冲突的文件，编辑完后，可以使用resolved来移除当地的冲突标记。
```
svn resolved xxx.php
```
