核心项目是由doPackage此方法开始位置在Main.cpp中
前期的准备工作：
* 检查aapt打包时的参数是否都存在
* 得到最终将资源打包输出到的APK名称
* 检查该文件是否存在不存在则创建，并确定其实常规文件
* 创建一个AaptAssets对象 assets = new AaptAssets();   
```assets = new AaptAssets(); ```  
然后正式进入打包过程  

打包过程如下：
## 1.收录AndroidManifest.xml文件目录assets和res下的资源目录和资源文件
解析目的：获得要编译资源的应用程序的包名称，拿到包名就可以创建ResourceTable对象


```cpp
int doPackage(Bundle*bundle) {
    sp<AaptAssets> assets;
    ...
    assets = new AaptAssets();
    ...    
    //将AndroidManifest.xml文件目录assets和res下的资源目录和资源文件收录起来保存到AaptAssets中的成员变量中    
    err = assets->slurpFromArgs(bundle);
}
```
```cpp
ssize_t AaptAssets::

slurpFromArgs(Bundle*bundle) {
    int count;
    int totalCount = 0;
    FileType type;
    // 获取res目录的路径
    const Vector<const char *>&resDirs = bundle -> getResourceSourceDirs();
    const size_t dirCount = resDirs.size();
    sp<AaptAssets> current = this;
    //获取bundle内所保存的aapt的命令选项个数，即要完成的功能个数
    const int N = bundle -> getFileSpecCount();
    /*     * 如果bundle中指定了AndroidManifest.xml文件，则首先包含它      */
    if (bundle -> getAndroidManifestFile() != NULL) {
        // place at root of zip.        
        String8 srcFile (bundle -> getAndroidManifestFile());
        //每向AaptAssets的对象中添加一个资源文件或者一个资源目录都要新建一个        
        // AaptGroupEntry的空对象并将其添加到一个类型为SortedVector的AaptAssets的成员变量mGroupEntries中        
        // 在这里调用addFile函数是将AndroidManifest.xml文件添加到成员变量mFiles中去.        
        addFile(srcFile.getPathLeaf(), AaptGroupEntry(), srcFile.getPathDir(), NULL, String8());
        //**        // 每添加一个资源就加1统计一次        
        totalCount++;
    }
    /*     
     * If a directory of custom assets was supplied, slurp 'em up.     
     * 判断是否指定了assets文件夹，如果指定则解析它      
     * 
     * */   
    const Vector<const char*>&assetDirs = bundle -> getAssetSourceDirs();//获取目录名称    
    const int AN = assetDirs.size();
    for (int i = 0; i < AN; i++) {
        FileType type = getFileType(assetDirs[i]);//获取目录类型        
        if (type == kFileTypeNonexistent) {
            fprintf(stderr, "ERROR: asset directory '%s' does not exist\n", assetDirs[i]);
            return UNKNOWN_ERROR;
        }
        if (type != kFileTypeDirectory) {
            fprintf(stderr, "ERROR: '%s' is not a directory\n", assetDirs[i]);
            return UNKNOWN_ERROR;
        }
        String8 assetRoot (assetDirs[i]);
        //创建一个名为”assets”的AaptDir对象        
        sp<AaptDir> assetAaptDir = makeDir(String8(kAssetDir));
        //assets        
        AaptGroupEntry group;
        //收录目录“assets”下的资源文件,并返回资源文件个数         
        count = assetAaptDir -> slurpFullTree(bundle, assetRoot, group, String8(), mFullAssetPaths, true);
        if (count < 0) {
            totalCount = count;            
            goto bail;
        }
        if (count > 0) {
            mGroupEntries.add(group);
        }
        totalCount += count;
        if (bundle -> getVerbose()) {
            printf("Found %d custom asset file%s in %s\n", count, (count == 1) ? "" : "s", assetDirs[i]);
        }
    }
    /*     * If a directory of resource-specific assets was supplied, slurp 'em up.     * 收录指定的res资源目录下的资源文件      */
    for (size_t i = 0; i < dirCount; i++) {        
        const char *res = resDirs[i];
        if (res) {
            type = getFileType(res);//获取文件类型            
            if (type == kFileTypeNonexistent) {
                fprintf(stderr, "ERROR: resource directory '%s' does not exist\n", res);
                return UNKNOWN_ERROR;
            }
            if (type == kFileTypeDirectory) {
                //如果指定了多个res资源目录文件, 则为其创建多个AaptAssets                
                // 类来分别收录这些目录中的信息，并将其设置赋值给当前AaptAssets对象的成员变量mOverlay                
                if (i > 0) {
                    sp<AaptAssets> nextOverlay = new AaptAssets();
                    current -> setOverlay(nextOverlay);
                    current = nextOverlay;
                    current -> setFullResPaths(mFullResPaths);
                }
                //调用成员函数slurpResourceTree来收录res目录下的资源文件                
                count = current -> slurpResourceTree(bundle, String8(res));
                if (i > 0 && count > 0) {
                    count = current -> filter(bundle);
                }
                if (count < 0) {
                    totalCount = count;                   
                    goto bail;
                }
                totalCount += count;//统计资源文件个数 
            } else {
                fprintf(stderr, "ERROR: '%s' is not a directory\n", res);
                return UNKNOWN_ERROR;
            }
        }
    }
    /*     * Now do any additional raw files.     * 接着收录剩余的指定的资源文件     */
    for (int arg = 0; arg < N; arg++) {        
        const char*assetDir = bundle -> getFileSpecEntry(arg);
        FileType type = getFileType(assetDir);
        if (type == kFileTypeNonexistent) {
            fprintf(stderr, "ERROR: input directory '%s' does not exist\n", assetDir);
            return UNKNOWN_ERROR;
        }
        if (type != kFileTypeDirectory) {
            fprintf(stderr, "ERROR: '%s' is not a directory\n", assetDir);
            return UNKNOWN_ERROR;
        }
        String8 assetRoot (assetDir);
        if (bundle -> getVerbose())
            printf("Processing raw dir '%s'\n", (const char*)assetDir);
        /*
            *  * Do a recursive traversal of subdir tree.  We don't make any
            * * guarantees about ordering, so we're okay with an inorder search
            * * using whatever order the OS happens to hand back to us.
            *
            */
        count = slurpFullTree(bundle, assetRoot, AaptGroupEntry(), String8(), mFullAssetPaths);
        if (count < 0) {
            /* failure; report error and remove archive */
            totalCount = count;           
            goto bail;
        }
        totalCount += count;
        if (bundle -> getVerbose())
            printf("Found %d asset file%s in %s\n", count, (count == 1) ? "" : "s", assetDir);
    }
    count = validate();
    if (count != NO_ERROR) {
        totalCount = count;       
        goto bail;
    }
    count = filter(bundle);
    if (count != NO_ERROR) {
        totalCount = count;       
        goto bail;
    }

    bail:
    return totalCount;
}
```

这里面的逻辑是： 
- 获取res目录的路径 
- 获取AndroidManifest.xml文件路径 
- 将AndroidManifest.xml文件添加到AaptAssets对象的成员变量mFiles中&添加到AaptGroupEntry的空对象并将其添加到一个类型为SortedVector的AaptAssets的成员变量mGroupEntries中 
- 判断是否指定了assets文件夹，如果指定则解析它 
- 收录指定的res资源目录下的资源文件 
- 接着收录剩余的指定的资源文件

```cpp
sp<AaptFile> AaptAssets::addFile(const String8& filePath,
	const AaptGroupEntry& entry,
	const String8& srcDir,
	sp<AaptGroup>* outGroup,
	const String8& resType) {
	sp<AaptDir> dir = this;
	//AaptAssets类继承了一个AaptDir类    
	sp<AaptGroup> group;
	sp<AaptFile> file;
	String8 root, remain(filePath), partialPath;
	while (remain.length() > 0) {
		//获取remain所描述文件的工作目录,如果其仅仅指定了文件名则返回文件名,如果文件名前添加了路径，则返回最上层的目录名        
		//例如，remain = “AndroidManifest.xml”，则root=“AndroidManifest.xml”, remain = “”;         
		//如果remain=“/rootpath/subpath/AndroidManifest.xml”,则，root=“rootpath”, remain=”subpath/AndroidManifest.xml”        
		root = remain.walkPath(&remain);
		partialPath.appendPath(root);
		const String8 rootStr(root);
		//在这里remain.length()返回0         
		if (remain.length() == 0) {
			//添加资源文件到mFiles中去           
			//dir指向当前AaptAssets对象,其调用getFiles返回类型为            
			//DefaultKeyVector<String8, sp<AaptGroup>>成员变量mFiles，判断其内部            
			//是否包含了名称为rootStr的AaptGroup对象，并返回其位置值            
			ssize_t i = dir->getFiles().indexOfKey(rootStr);
			//如果返回的位置值>=0表示mFiles中已经包含了这个名为rootStr的            
			//AaptGroup对象，则将group指向该对象, 否则新建一个名称为rootStr            
			//的AaptGroup对象并添加到mFiles中去            
			if (i >= 0) {
				group = dir->getFiles().valueAt(i);
			}
			else {
				group = new AaptGroup(rootStr, filePath);
				status_t res = dir->addFile(rootStr, group);
				if (res != NO_ERROR) {
					return NULL;
				}
			}
			// 新建一个AaptFile对象指向需要添加的源文件, 并将该AaptFile对象            
			// 添加到类型为DefaultKeyedVector<AaptGroupEntry, sp<AaptFile> >的            
			// AaptGroup的成员变量 mFiles中去            
			file = new AaptFile(srcDir.appendPathCopy(filePath), entry, resType);
			status_t res = group->addFile(file);
			if (res != NO_ERROR) {
				return NULL;
			}
			break;
		}
		else {
			//添加资源目录到mDirs中去           
			//dir指向当前AaptAssets对象,其调用getDirs返回类型为            
			//DefaultKeyVector<String8, sp<AaptDir>>成员变量mDirs，判断其内部            
			//是否包含了名称为rootStr的AaptDir对象，并返回其位置值            
			ssize_t i = dir->getDirs().indexOfKey(rootStr);
			//如果返回的位置值>=0表示mDirs中已经包含了这个名为rootStr的            
			//AaptDir对象，则将dir指向该对象，否则新建一个名称为rootStr的AaptDir对象并添加到mDirs中去            
			if (i >= 0) {
				dir = dir->getDirs().valueAt(i);
			}
			else {
				sp<AaptDir> subdir = new AaptDir(rootStr, partialPath);
				status_t res = dir->addDir(rootStr, subdir);
				if (res != NO_ERROR) {
					return NULL;
				}
				dir = subdir;
			}
		}
	}
	//将一个空的AaptGroupEntry对象添加到mGroupEntries中去，其是一个SortedVector    
	mGroupEntries.add(entry);
	if (outGroup)
		*outGroup = group;
	return file;
}
```
这里面核心的逻辑是： 
- 传入AndroidManifest.xml相关路径 
- 将相关路径进行拆分 
- 封装成一个group = new AaptGroup(rootStr, filePath);对象 
- 将AaptGroup对象添加到AaptAssets的成员变量mFiles中 
- 新建一个AaptFile对象，参数指向入AndroidManifest.xml相关路径 
- 将AaptFile添加到AaptGroup的mFiles中

解析assets中的文件
```cpp
String8 assetRoot(assetDirs[i]);
//创建一个名为”assets”的AaptDir对象
sp<AaptDir> assetAaptDir = makeDir(String8(kAssetDir));//assets
AaptGroupEntry group;
//收录目录“assets”下的资源文件,并返回资源文件个数 
count = assetAaptDir->slurpFullTree(bundle, assetRoot, group, String8(), mFullAssetPaths, true);
```
```cpp
/收录路径名为srcDir目录下的所有资源文件，并将对应目录下的文件名都保存到fullResPaths中去
ssize_t AaptAssets::slurpFullTree(Bundle* bundle, const String8& srcDir, const AaptGroupEntry& kind, const String8& resType,
	sp<FilePathStore>& fullResPaths, const bool overwrite){    
	//接着调用父类中的AaptDir的成员函数slurpFullTree收录srcDir中的资源文件     
	ssize_t res = AaptDir::slurpFullTree(bundle, srcDir, kind, resType, fullResPaths, overwrite);    
	if (res > 0) {        
		//如果收录的资源个数>0，则将其归为一类，为这类资源文件创建一个对应        
		//AaptGroupEntry对象并添加到对应的成员变量mGroupEntries中去        
		mGroupEntries.add(kind);    
	}    
	return res;
}
```
```cpp
ssize_t AaptDir::slurpFullTree(Bundle* bundle, 
	const String8& srcDir, const AaptGroupEntry& kind, 
	const String8& resType, sp<FilePathStore>& fullResPaths, const bool overwrite) {
	Vector<String8> fileNames; 
	{        
		DIR* dir = NULL;        
		// 首先打开将要收录的资源文件所在的源目录         
		dir = opendir(srcDir.string());        
		if (dir == NULL) {            
			fprintf(stderr, "ERROR: opendir(%s): %s\n", srcDir.string(), strerror(errno));            
			return UNKNOWN_ERROR;        
		}        
		/*         
		* Slurp the filenames out of the directory.         
		* 遍历srcDir目录下的每一个资源文件，将其添加到AaptAssets的成员变量         
		* mFullAssetPaths中，其继承了一个Vector<String8>          
		*/        
		while (1) {            
			struct dirent* entry;            
			entry = readdir(dir);            
			if (entry == NULL)                
				break;            
			if (isHidden(srcDir.string(), entry->d_name))                
				continue;            
			String8 name(entry->d_name);            
			fileNames.add(name);            
			// Add fully qualified path for dependency purposes            
			// if we're collecting them            
			// 按照全部路径将资源文件添加到fullResPaths中去            
			if (fullResPaths != NULL) {                
				fullResPaths->add(srcDir.appendPathCopy(name));            
			}       
		}        
		closedir(dir);    
	}    
	ssize_t count = 0;    
	/*     
	* Stash away the files and recursively descend into subdirectories.     
	* 递归解析srcDir下的子目录中的资源文件，直到收录完所有的目录中的资源文件为止     
	*/    
	const size_t N = fileNames.size();    
	size_t i;    
	for (i = 0; i < N; i++) {        
		String8 pathName(srcDir);        
		FileType type;        
		pathName.appendPath(fileNames[i].string());        
		type = getFileType(pathName.string());        
		//如果是资源子目录，并且其尚未收录在mDirs中，则为其创建一个        
		//AaptDir对象，继续递归遍历其中的资源文件及目录        
		if (type == kFileTypeDirectory) {            
			sp<AaptDir> subdir;            
			bool notAdded = false;            
			if (mDirs.indexOfKey(fileNames[i]) >= 0) {                
				subdir = mDirs.valueFor(fileNames[i]);            
			} else {                
				subdir = new AaptDir(fileNames[i], mPath.appendPathCopy(fileNames[i]));                
				notAdded = true;            
			}            
			ssize_t res = subdir->slurpFullTree(bundle, pathName, kind, resType, fullResPaths, overwrite);            
			if (res < NO_ERROR) {                
				return res;            
			}            
			if (res > 0 && notAdded) {                
				mDirs.add(fileNames[i], subdir);//将资源目录添加到mDirs变量中            
			}            
			count += res;        
			// 如果其为一个资源文件，则为其创建一个指定的AaptFile变量        
			//并为其创建一个对应的AaptGroup变量, 将这个AaptGroup变量添加        
			//到mFiles变量中，然后将AaptFile变量添加到AaptGroup中去        
		} else if (type == kFileTypeRegular) {            
			sp<AaptFile> file = new AaptFile(pathName, kind, resType);            
			status_t err = addLeafFile(fileNames[i], file, overwrite);            
			if (err != NO_ERROR) {                
				return err;            
			}        
			//返回总的资源文件个数            
			count++;        
		} else {            
			if (bundle->getVerbose())                
				printf("   (ignoring non-file/dir '%s')\n", pathName.string());        
		}    
	}    
	return count;
}
```
* 遍历assets中的所有文件和目录将所有绝对路径添加到fullResPaths中这个对应的是AaptAssets的成员变量mFullAssetPaths
* 将资源目录添加到成员变量mDirs中DefaultKeyedVector<String8, sp<AaptDir> > mDirs;
* 如果是一个资源文件则为其创建一个指定的AaptFile变量并为其创建一个对应AaptGroup变量，将这个AaptGroup对象添加到mFiles变量中，然后将AaptFile变量添加到AaptGroup中去

```cpp
/* 
* If a directory of resource-specific assets was supplied, slurp 'em up. 
* 收录指定的res资源目录下的资源文件  
*/
for (size_t i = 0; i<dirCount; i++) {
	const char *res = resDirs[i];    
	if (res) {
		type = getFileType(res);//获取文件类型        
		if (type == kFileTypeNonexistent) {            
			fprintf(stderr, "ERROR: resource directory '%s' does not exist\n", res);            
			return UNKNOWN_ERROR;        
		}        
		if (type == kFileTypeDirectory) {            
			//如果指定了多个res资源目录文件, 则为其创建多个AaptAssets            
			//类来分别收录这些目录中的信息，并将其设置赋值给当前AaptAssets对象的成员变量mOverlay            
			if (i>0) {                
				sp<AaptAssets> nextOverlay = new AaptAssets();                
				current->setOverlay(nextOverlay);                
				current = nextOverlay;                
				current->setFullResPaths(mFullResPaths);            
			}            
			//调用成员函数slurpResourceTree来收录res目录下的资源文件            
			count = current->slurpResourceTree(bundle, String8(res));            
			if (i > 0 && count > 0) {              
				count = current->filter(bundle);            
			}            
			if (count < 0) {                
				totalCount = count;                
				goto bail;           
			}            
			totalCount += count;//统计资源文件个数        
		} else {            
			fprintf(stderr, "ERROR: '%s' is not a directory\n", res);            
			return UNKNOWN_ERROR;        
		}    
	}
}
```

```cpp
status_t buildResources(Bundle* bundle, const sp<AaptAssets>& assets, sp<ApkBuilder>& builder) {
	// First, look for a package file to parse.  This is required to    
	// be able to generate the resource information.    
	//首先从assets中获取AndroidManifest.xml文件的信息    
	//AndroidManifest.xml文件信息是保存在assets的成员变量mFiles中的，    
	//但是其被封装成一个AaptFile类对象保存在AaptGroup对象最终再保存到mFiles中的    
	sp<AaptGroup> androidManifestFile = assets->getFiles().valueFor(String8("AndroidManifest.xml"));
	if (androidManifestFile == NULL) {
		fprintf(stderr, "ERROR: No AndroidManifest.xml file found.\n");
		return UNKNOWN_ERROR;
	}
	//解析AndroidManifest.xml文件    
	status_t err = parsePackage(bundle, assets, androidManifestFile);
	if (err != NO_ERROR) {
		return err;
	}
	if (kIsDebug) {
		printf("Creating resources for package %s\n", assets->getPackage().string());
	}
	ResourceTable::PackageType packageType = ResourceTable::App;
	if (bundle->getBuildSharedLibrary()) {
		packageType = ResourceTable::SharedLibrary;
	}
	else if (bundle->getExtending()) {
		packageType = ResourceTable::System;
	}
	else if (!bundle->getFeatureOfPackage().isEmpty()) {
		packageType = ResourceTable::AppFeature;
	}
	//根据包名创建一个对应的ResourceTable ,在上面解析AndroidManifest.xml中解析的包名    
	ResourceTable table(bundle, String16(assets->getPackage()), packageType);
	//添加被引用资源包，比如系统的那些android:命名空间下的资源也就是android.jar   
	err = table.addIncludedResources(bundle, assets);
	if (err != NO_ERROR) {
		return err;
	}
	if (kIsDebug) {
		printf("Found %d included resource packages\n", (int)table.size());
	}
	// Standard flags for compiled XML and optional UTF-8 encoding   
	//设置编译XML文件的选项为标准和UTF-8的编码方式    
	int xmlFlags = XML_COMPILE_STANDARD_RESOURCE;
	/*
	 * Only enable UTF-8 if the caller of aapt didn't specifically
	 * request UTF-16 encoding and the parameters of this package
	 * allow UTF-8 to be used.
	 */
	if (!bundle->getUTF16StringsOption()) {
		xmlFlags |= XML_COMPILE_UTF8;
	}
	// --------------------------------------------------------------    
	// First, gather all resource information.   
	// --------------------------------------------------------------    
	// resType -> leafName -> group    
	KeyedVector<String8, sp<ResourceTypeSet> > *resources = new KeyedVector<String8, sp<ResourceTypeSet> >;
	//调用collect_files将前面收集到assets中的各类资源文件重新收集到resources中来       
	collect_files(assets, resources);
	//定义收集各类资源文件的容器    
	sp<ResourceTypeSet> drawables;
	sp<ResourceTypeSet> layouts;
	sp<ResourceTypeSet> anims;
	sp<ResourceTypeSet> animators;
	sp<ResourceTypeSet> interpolators;
	sp<ResourceTypeSet> transitions;
	sp<ResourceTypeSet> xmls;
	sp<ResourceTypeSet> raws;
	sp<ResourceTypeSet> colors;
	sp<ResourceTypeSet> menus;
	sp<ResourceTypeSet> mipmaps;
	//将保存到resources中的各类文件的Set保存到我们上述定义的Set中去    
	ASSIGN_IT(drawable);
	ASSIGN_IT(layout);
	ASSIGN_IT(anim);
	ASSIGN_IT(animator);
	ASSIGN_IT(interpolator);
	ASSIGN_IT(transition);
	ASSIGN_IT(xml);
	ASSIGN_IT(raw);
	ASSIGN_IT(color);
	ASSIGN_IT(menu);
	ASSIGN_IT(mipmap);
	//设置assets的资源为resources中保存的    
	assets->setResources(resources);
	// now go through any resource overlays and collect their files    
	//判断当前应用程序是否有overlay的资源，有的话将assets中保存的资源设置为overlay中   
	sp<AaptAssets> current = assets->getOverlay();
	while (current.get()) {
		KeyedVector<String8, sp<ResourceTypeSet> > *resources = new KeyedVector<String8, sp<ResourceTypeSet> >;
		current->setResources(resources);
		collect_files(current, resources);
		current = current->getOverlay();
	}
		
	// apply the overlay files to the base set   
	//如果有overlay资源则使用overlay资源替换现有资源    
	if (!applyFileOverlay(bundle, assets, &drawables, "drawable") ||
		!applyFileOverlay(bundle, assets, &layouts, "layout") ||
		!applyFileOverlay(bundle, assets, &anims, "anim") ||
		!applyFileOverlay(bundle, assets, &animators, "animator") ||
		!applyFileOverlay(bundle, assets, &interpolators, "interpolator") ||
		!applyFileOverlay(bundle, assets, &transitions, "transition") ||
		!applyFileOverlay(bundle, assets, &xmls, "xml") ||
		!applyFileOverlay(bundle, assets, &raws, "raw") ||
		!applyFileOverlay(bundle, assets, &colors, "color") ||
		!applyFileOverlay(bundle, assets, &menus, "menu") ||
		!applyFileOverlay(bundle, assets, &mipmaps, "mipmap")) {
		return UNKNOWN_ERROR;
	}
	bool hasErrors = false;
	//如果当前应用程序有drawables资源，则首先调用preProcessImages函数预处理   
	//图像，然后调用makeFileResources函数处理drawables中的资源    
	if (drawables != NULL) {
		if (bundle->getOutputAPKFile() != NULL) {
			//预处理图像, 目前只支持处理png格式图像            
			err = preProcessImages(bundle, assets, drawables, "drawable");
		}
		if (err == NO_ERROR) {
			//处理drawables中的资源            
			//我们分析如何将收集到一个AaptAsset中的资源文件信息分类重新由函数makeFileResources组织到一个ResourceTable对象           
			//中去，这些资源文件的信息最终组织在Package, Type, Entry, Item中，Package代表当前编译APK的包信息，            
			//Type类保存资源类型信息, Entry代表保存资源文件，Item保存文件中属性信息. Package包含Type, Type包含Entry,            
			//Entry包含Item.            
			err = makeFileResources(bundle, assets, &table, drawables, "drawable");
			if (err != NO_ERROR) {
				hasErrors = true;
			}
		}
		else {
			hasErrors = true;
		}
	}
	if (mipmaps != NULL) {
		if (bundle->getOutputAPKFile() != NULL) {
			err = preProcessImages(bundle, assets, mipmaps, "mipmap");
		}
		if (err == NO_ERROR) {
			err = makeFileResources(bundle, assets, &table, mipmaps, "mipmap");
			if (err != NO_ERROR) {
				hasErrors = true;
			}
		}
		else {
			hasErrors = true;
		}
	}
	if (layouts != NULL) {
		err = makeFileResources(bundle, assets, &table, layouts, "layout");
		if (err != NO_ERROR) {
			hasErrors = true;
		}
	}
	if (anims != NULL) {
		err = makeFileResources(bundle, assets, &table, anims, "anim");
		if (err != NO_ERROR) {
			hasErrors = true;
		}
	}
	if (animators != NULL) {
		err = makeFileResources(bundle, assets, &table, animators, "animator");
		if (err != NO_ERROR) {
			hasErrors = true;
		}
	}
	if (transitions != NULL) {
		err = makeFileResources(bundle, assets, &table, transitions, "transition");
		if (err != NO_ERROR) {
			hasErrors = true;
		}
	}
	if (interpolators != NULL) {
		err = makeFileResources(bundle, assets, &table, interpolators, "interpolator");
		if (err != NO_ERROR) {
			hasErrors = true;
		}
	}
	if (xmls != NULL) {
		err = makeFileResources(bundle, assets, &table, xmls, "xml");
		if (err != NO_ERROR) {
			hasErrors = true;
		}
	}
	if (raws != NULL) {
		err = makeFileResources(bundle, assets, &table, raws, "raw");
		if (err != NO_ERROR) {
			hasErrors = true;
		}
	}
	// compile resources    
	current = assets;
	while (current.get()) {
		KeyedVector<String8, sp<ResourceTypeSet> > *resources = current->getResources();
		ssize_t index = resources->indexOfKey(String8("values"));
		if (index >= 0) {
			ResourceDirIterator it(resources->valueAt(index), String8("values"));
			ssize_t res;
			while ((res = it.next()) == NO_ERROR) {
				sp<AaptFile> file = it.getFile();
				//对于values则是由这个独立的函数进行组织的,将解析完的数据保存在变量table中                
				res = compileResourceFile(bundle, assets, file, it.getParams(), (current != assets), &table);
				if (res != NO_ERROR) {
					hasErrors = true;
				}
			}
		}
		current = current->getOverlay();
	}
	if (colors != NULL) {
		err = makeFileResources(bundle, assets, &table, colors, "color");
		if (err != NO_ERROR) {
			hasErrors = true;
		}
	}
	if (menus != NULL) {
		err = makeFileResources(bundle, assets, &table, menus, "menu");
		if (err != NO_ERROR) {
			hasErrors = true;
		}
	}
	// --------------------------------------------------------------------    
	// Assignment of resource IDs and initial generation of resource table.    
	// --------------------------------------------------------------------    
	//到目前为止上面的工作我们将当前正在编译的应用程序所依赖的所有资源文件信息(包括系统android.jar中的和   
	//应用程序自身的被收集到一个AaptAsset类对象中的)都收集到了一个ResourceTable对象中去了，    
	//接下来buildResources函数的工作是为这些资源文件中的各种属性分配资源ID    
	//下面我们就开始分配Bag资源ID    
	// 调用ResourceTable类的成员函数assignResourceIds分配bag资源ID信息    
	if (table.hasResources()) {
		err = table.assignResourceIds();
		if (err < NO_ERROR) {
			return err;
		}
	}
	// --------------------------------------------------------------    
	// Finally, we can now we can compile XML files, which may reference    
	// resources.    
	// --------------------------------------------------------------    
	// 最后我们将要编译XML文件，这样我们就能引用资源    
	if (layouts != NULL) {
		ResourceDirIterator it(layouts, String8("layout"));
		while ((err = it.next()) == NO_ERROR) {
			String8 src = it.getFile()->getPrintableSource();
			//对于对于anim, animator, interpolator, xml, color, menu, drawable中的xml文件都是通过compileXmlFile函数进行编译的.            
			//在这里面用XMLNode::assignResourceIds里面给每个属性赋值            
			err = compileXmlFile(bundle, assets, String16(it.getBaseName()), it.getFile(), &table, xmlFlags);
			if (err == NO_ERROR) {
				ResXMLTree block;
				//将编译后的信息组织到ResXMLTree中去                 
				block.setTo(it.getFile()->getData(), it.getFile()->getSize(), true);
				//检验分配的ID是否正确                
				checkForIds(src, block);
			}
			else {
				hasErrors = true;
			}
		}
		if (err < NO_ERROR) {
			hasErrors = true;
		}
		err = NO_ERROR;
	}
	if (anims != NULL) {
		ResourceDirIterator it(anims, String8("anim"));
		while ((err = it.next()) == NO_ERROR) {
			err = compileXmlFile(bundle, assets, String16(it.getBaseName()), it.getFile(), &table, xmlFlags);
			if (err != NO_ERROR) {
				hasErrors = true;
			}
		}
		if (err < NO_ERROR) {
			hasErrors = true;
		}
		err = NO_ERROR;
	}
	if (animators != NULL) {
		ResourceDirIterator it(animators, String8("animator"));
		while ((err = it.next()) == NO_ERROR) {
			err = compileXmlFile(bundle, assets, String16(it.getBaseName()), it.getFile(), &table, xmlFlags);
			if (err != NO_ERROR) {
				hasErrors = true;
			}
		}
		if (err < NO_ERROR) {
			hasErrors = true;
		}
		err = NO_ERROR;
	}
	if (interpolators != NULL) {
		ResourceDirIterator it(interpolators, String8("interpolator"));
		while ((err = it.next()) == NO_ERROR) {
			err = compileXmlFile(bundle, assets, String16(it.getBaseName()), it.getFile(), &table, xmlFlags);
			if (err != NO_ERROR) {
				hasErrors = true;
			}
		}
		if (err < NO_ERROR) {
			hasErrors = true;
		}
		err = NO_ERROR;
	}
	if (transitions != NULL) {
		ResourceDirIterator it(transitions, String8("transition"));
		while ((err = it.next()) == NO_ERROR) {
			err = compileXmlFile(bundle, assets, String16(it.getBaseName()), it.getFile(), &table, xmlFlags);
			if (err != NO_ERROR) {
				hasErrors = true;
			}
		}
		if (err < NO_ERROR) {
			hasErrors = true;
		}
		err = NO_ERROR;
	}
	if (xmls != NULL) {
		ResourceDirIterator it(xmls, String8("xml"));
		while ((err = it.next()) == NO_ERROR) {
			err = compileXmlFile(bundle, assets, String16(it.getBaseName()), it.getFile(), &table, xmlFlags);
			if (err != NO_ERROR) {
				hasErrors = true;
			}
		}
		if (err < NO_ERROR) {
			hasErrors = true;
		}
		err = NO_ERROR;
	}
	if (drawables != NULL) {
		ResourceDirIterator it(drawables, String8("drawable"));
		while ((err = it.next()) == NO_ERROR) {
			err = postProcessImage(bundle, assets, &table, it.getFile());
			if (err != NO_ERROR) {
				hasErrors = true;
			}
		}
		if (err < NO_ERROR) {
			hasErrors = true;
		}
		err = NO_ERROR;
	}
	if (colors != NULL) {
		ResourceDirIterator it(colors, String8("color"));
		while ((err = it.next()) == NO_ERROR) {
			err = compileXmlFile(bundle, assets, String16(it.getBaseName()), it.getFile(), &table, xmlFlags);
			if (err != NO_ERROR) {
				hasErrors = true;
			}
		}
		if (err < NO_ERROR) {
			hasErrors = true;
		}
		err = NO_ERROR;
	}
	if (menus != NULL) {
		ResourceDirIterator it(menus, String8("menu"));
		while ((err = it.next()) == NO_ERROR) {
			String8 src = it.getFile()->getPrintableSource();
			err = compileXmlFile(bundle, assets, String16(it.getBaseName()), it.getFile(), &table, xmlFlags);
			if (err == NO_ERROR) {
				ResXMLTree block;
				block.setTo(it.getFile()->getData(), it.getFile()->getSize(), true);
				checkForIds(src, block);
			}
			else {
				hasErrors = true;
			}
		}
		if (err < NO_ERROR) {
			hasErrors = true;
		}
		err = NO_ERROR;
	}
	// Now compile any generated resources.   
	std::queue<CompileResourceWorkItem>& workQueue = table.getWorkQueue();
	while (!workQueue.empty()) {
		CompileResourceWorkItem& workItem = workQueue.front();
		err = compileXmlFile(bundle, assets, workItem.resourceName, workItem.file, &table, xmlFlags);
		if (err == NO_ERROR) {
			assets->addResource(workItem.resPath.getPathLeaf(), workItem.resPath, workItem.file, workItem.file->getResourceType());
		}
		else {
			hasErrors = true;
		}
		workQueue.pop();
	}
	if (table.validateLocalizations()) {
		hasErrors = true;
	}
	if (hasErrors) {
		return UNKNOWN_ERROR;
	}
	// If we're not overriding the platform build versions,   
	// extract them from the platform APK.   
	if (packageType != ResourceTable::System &&
		(bundle->getPlatformBuildVersionCode() == "" || bundle->getPlatformBuildVersionName() == "")) {
		err = extractPlatformBuildVersion(assets->getAssetManager(), bundle);
		if (err != NO_ERROR) {
			return UNKNOWN_ERROR;
		}
	}
	//下面这些代码是产生经过flatten的AndroidManifest.xml文件    
	// 取出AndroidManifest.xml文件    
	const sp<AaptFile> manifestFile(androidManifestFile->getFiles().valueAt(0));
	String8 manifestPath(manifestFile->getPrintableSource());
	// Generate final compiled manifest file.    
	//清空manifestFile所指向的AndroidManfiest.xml的信息，然后重新解析    
	manifestFile->clearData();
	sp<XMLNode> manifestTree = XMLNode::parse(manifestFile);
	if (manifestTree == NULL) {
		return UNKNOWN_ERROR;
	}
	//检测是否AndroidManifest.xml中是否有overlay资源，如果有就将现有资源替换   
	err = massageManifest(bundle, manifestTree);
	if (err < NO_ERROR) {
		return err;
	}
	//编译AndroidManifest.xml文件    
	err = compileXmlFile(bundle, assets, String16(), manifestTree, manifestFile, &table);
	if (err < NO_ERROR) {
		return err;
	}
	if (table.modifyForCompat(bundle) != NO_ERROR) {
		return UNKNOWN_ERROR;
	}
	//block.restart();    
	//printXMLBlock(&block);   
	// --------------------------------------------------------------  
	// Generate the final resource table.   
	// Re-flatten because we may have added new resource IDs   
	// --------------------------------------------------------------    
	ResTable finalResTable;
	sp<AaptFile> resFile;
	if (table.hasResources()) {
		//生成资源符号表      
		sp<AaptSymbols> symbols = assets->getSymbolsFor(String8("R"));
		err = table.addSymbols(symbols, bundle->getSkipSymbolsWithoutDefaultLocalization());
		if (err < NO_ERROR) {
			return err;
		}
		KeyedVector<Symbol, Vector<SymbolDefinition> > densityVaryingResources;
		if (builder->getSplits().size() > 1) {
			// Only look for density varying resources if we're generating           
			// splits.           
			table.getDensityVaryingResources(densityVaryingResources);
		}
		Vector<sp<ApkSplit> >& splits = builder->getSplits();
		const size_t numSplits = splits.size();
		for (size_t i = 0; i < numSplits; i++) {
			sp<ApkSplit>& split = splits.editItemAt(i);
			// 生成资源索引表          
			sp<AaptFile> flattenedTable = new AaptFile(String8("resources.arsc"), AaptGroupEntry(), String8());
			//ResourceTable::flatten用于生成资源索引表resources.arsc          
			err = table.flatten(bundle, split->getResourceFilter(), flattenedTable, split->isBase());
			if (err != NO_ERROR) {
				fprintf(stderr, "Failed to generate resource table for split '%s'\n", split->getPrintableName().string());
				return err;
			}
			split->addEntry(String8("resources.arsc"), flattenedTable);
			if (split->isBase()) {
				resFile = flattenedTable;
				err = finalResTable.add(flattenedTable->getData(), flattenedTable->getSize());
				if (err != NO_ERROR) {
					fprintf(stderr, "Generated resource table is corrupt.\n");
					return err;
				}
			}
			else {
				ResTable resTable;
				err = resTable.add(flattenedTable->getData(), flattenedTable->getSize());
				if (err != NO_ERROR) {
					fprintf(stderr, "Generated resource table for split '%s' is corrupt.\n", split->getPrintableName().string());
					return err;
				}
				bool hasError = false;
				const std::set<ConfigDescription>& splitConfigs = split->getConfigs();
				for (std::set<ConfigDescription>::const_iterator iter = splitConfigs.begin(); iter != splitConfigs.end(); ++iter) {
					const ConfigDescription& config = *iter;
					if (AaptConfig::isDensityOnly(config)) {
						// Each density only split must contain all               
						// density only resources.       
						Res_value val;
						resTable.setParameters(&config);
						const size_t densityVaryingResourceCount = densityVaryingResources.size();
						for (size_t k = 0; k < densityVaryingResourceCount; k++) {
							const Symbol& symbol = densityVaryingResources.keyAt(k);
							ssize_t block = resTable.getResource(symbol.id, &val, true);
							if (block < 0) {
								// Maybe it's in the base?         
								finalResTable.setParameters(&config);
								block = finalResTable.getResource(symbol.id, &val, true);
							}
							if (block < 0) {
								hasError = true;
								SourcePos().error("%s has no definition for density split '%s'", symbol.toString().string(), config.toString().string());                                if (bundle->getVerbose()) {
									const Vector<SymbolDefinition>& defs = densityVaryingResources[k];
									const size_t defCount = std::min(size_t(5), defs.size());
									for (size_t d = 0; d < defCount; d++) {
										const SymbolDefinition& def = defs[d];
										def.source.error("%s has definition for %s", symbol.toString().string(), def.config.toString().string());
									}
									if (defCount < defs.size()) {
										SourcePos().error("and %d more ...", (int)(defs.size() - defCount));
									}
								}
							}
						}
					}
				}
				if (hasError) {
					return UNKNOWN_ERROR;
				}
				// Generate the AndroidManifest for this split.    
				sp<AaptFile> generatedManifest = new AaptFile(String8("AndroidManifest.xml"), AaptGroupEntry(), String8());
				err = generateAndroidManifestForSplit(bundle, assets, split, generatedManifest, &table);
				if (err != NO_ERROR) {
					fprintf(stderr, "Failed to generate AndroidManifest.xml for split '%s'\n", split->getPrintableName().string());
					return err;
				}
				split->addEntry(String8("AndroidManifest.xml"), generatedManifest);
			}
		}
		if (bundle->getPublicOutputFile()) {
			FILE* fp = fopen(bundle->getPublicOutputFile(), "w+");
			if (fp == NULL) {
				fprintf(stderr, "ERROR: Unable to open public definitions output file %s: %s\n", (const char*)bundle->getPublicOutputFile(), strerror(errno));
				return UNKNOWN_ERROR;
			}
			if (bundle->getVerbose()) {
				printf("  Writing public definitions to %s.\n", bundle->getPublicOutputFile());
			}
			table.writePublicDefinitions(String16(assets->getPackage()), fp);
			fclose(fp);
		}
		if (finalResTable.getTableCount() == 0 || resFile == NULL) {
			fprintf(stderr, "No resource table was generated.\n");
			return UNKNOWN_ERROR;
		}
	}
	// Perform a basic validation of the manifest file.  This time we   
	// parse it with the comments intact, so that we can use them to   
	// generate java docs...  so we are not going to write this one    
	// back out to the final manifest data.  
	sp<AaptFile> outManifestFile = new AaptFile(manifestFile->getSourceFile(), manifestFile->getGroupEntry(), manifestFile->getResourceType());
	err = compileXmlFile(bundle, assets, String16(), manifestFile, outManifestFile, &table, XML_COMPILE_ASSIGN_ATTRIBUTE_IDS | XML_COMPILE_STRIP_WHITESPACE | XML_COMPILE_STRIP_RAW_VALUES);
	if (err < NO_ERROR) {
		return err;
	}
	ResXMLTree block;
	block.setTo(outManifestFile->getData(), outManifestFile->getSize(), true);
	String16 manifest16("manifest");
	String16 permission16("permission");
	String16 permission_group16("permission-group");
	String16 uses_permission16("uses-permission");
	String16 instrumentation16("instrumentation");
	String16 application16("application");
	String16 provider16("provider");
	String16 service16("service");
	String16 receiver16("receiver");
	String16 activity16("activity");
	String16 action16("action");
	String16 category16("category");
	String16 data16("scheme");
	String16 feature_group16("feature-group");
	String16 uses_feature16("uses-feature");
	const char* packageIdentChars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ._0123456789";
	const char* packageIdentCharsWithTheStupid = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ._0123456789-";
	const char* classIdentChars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ._0123456789$";
	const char* processIdentChars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ._0123456789:";
	const char* authoritiesIdentChars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ._0123456789-:;";
	const char* typeIdentChars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ._0123456789:-/*+";
	const char* schemeIdentChars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ._0123456789-";
	ResXMLTree::event_code_t code;
	sp<AaptSymbols> permissionSymbols;
	sp<AaptSymbols> permissionGroupSymbols;
	while ((code = block.next()) != ResXMLTree::END_DOCUMENT
		&& code > ResXMLTree::BAD_DOCUMENT) {
		if (code == ResXMLTree::START_TAG) {
			size_t len;
			if (block.getElementNamespace(&len) != NULL) {
				continue;
			}
			if (strcmp16(block.getElementName(&len), manifest16.string()) == 0) {
				if (validateAttr(manifestPath, finalResTable, block, NULL, "package", packageIdentChars, true) != ATTR_OKAY) {
					hasErrors = true;
				}
				if (validateAttr(manifestPath, finalResTable, block, RESOURCES_ANDROID_NAMESPACE, "sharedUserId", packageIdentChars, false) != ATTR_OKAY) {
					hasErrors = true;
				}
			}
			else if (strcmp16(block.getElementName(&len), permission16.string()) == 0
				|| strcmp16(block.getElementName(&len), permission_group16.string()) == 0) {
				const bool isGroup = strcmp16(block.getElementName(&len), permission_group16.string()) == 0;
				if (validateAttr(manifestPath, finalResTable, block, RESOURCES_ANDROID_NAMESPACE, "name", isGroup ? packageIdentCharsWithTheStupid : packageIdentChars, true) != ATTR_OKAY) {
					hasErrors = true;
				}
				SourcePos srcPos(manifestPath, block.getLineNumber());
				sp<AaptSymbols> syms;
				if (!isGroup) {
					syms = permissionSymbols;
					if (syms == NULL) {
						sp<AaptSymbols> symbols = assets->getSymbolsFor(String8("Manifest"));
						syms = permissionSymbols = symbols->addNestedSymbol(
							String8("permission"), srcPos);
					}
				}
				else {
					syms = permissionGroupSymbols;
					if (syms == NULL) {
						sp<AaptSymbols> symbols = assets->getSymbolsFor(String8("Manifest"));
						syms = permissionGroupSymbols = symbols->addNestedSymbol(
							String8("permission_group"), srcPos);
					}
				}
				size_t len;
				ssize_t index = block.indexOfAttribute(RESOURCES_ANDROID_NAMESPACE, "name");
				const char16_t* id = block.getAttributeStringValue(index, &len);
				if (id == NULL) {
					fprintf(stderr, "%s:%d: missing name attribute in element <%s>.\n", manifestPath.string(), block.getLineNumber(), String8(block.getElementName(&len)).string());
					hasErrors = true;
					break;
				}
				String8 idStr(id);
				char* p = idStr.lockBuffer(idStr.size());
				char* e = p + idStr.size();
				bool begins_with_digit = true;
				// init to true so an empty string fails         
				while (e > p) {
					e--;
					if (*e >= '0' && *e <= '9') {
						begins_with_digit = true;
						continue;
					}
					if ((*e >= 'a' && *e <= 'z') ||
						(*e >= 'A' && *e <= 'Z') ||
						(*e == '_')) {
						begins_with_digit = false;
						continue;
					}
					if (isGroup && (*e == '-')) {
						*e = '_';
						begins_with_digit = false;
						continue;
					}
					e++;
					break;
				}
				idStr.unlockBuffer();
				// verify that we stopped because we hit a period or        
				// the beginning of the string, and that the            
				// identifier didn't begin with a digit.            
				if (begins_with_digit || (e != p && *(e - 1) != '.')) {
					fprintf(stderr, "%s:%d: Permission name <%s> is not a valid Java symbol\n", manifestPath.string(), block.getLineNumber(), idStr.string());                  hasErrors = true;
				}                syms->addStringSymbol(String8(e), idStr, srcPos);                const char16_t* cmt = block.getComment(&len);
				if (cmt != NULL && *cmt != 0) {
					//printf("Comment of %s: %s\n", String8(e).string(),                    //        
					String8(cmt).string());
					syms->appendComment(String8(e), String16(cmt), srcPos);
				}
				else {
					//printf("No comment for %s\n", String8(e).string());          
				}
				syms->makeSymbolPublic(String8(e), srcPos);
			}
			else if (strcmp16(block.getElementName(&len), uses_permission16.string()) == 0) {
				if (validateAttr(manifestPath, finalResTable, block, RESOURCES_ANDROID_NAMESPACE, "name", packageIdentChars, true) != ATTR_OKAY) {
					hasErrors = true;
				}
			}
			else if (strcmp16(block.getElementName(&len), instrumentation16.string()) == 0) {
				if (validateAttr(manifestPath, finalResTable, block, RESOURCES_ANDROID_NAMESPACE, "name", classIdentChars, true) != ATTR_OKAY) {
					hasErrors = true;
				}
				if (validateAttr(manifestPath, finalResTable, block, RESOURCES_ANDROID_NAMESPACE, "targetPackage", packageIdentChars, true) != ATTR_OKAY) {
					hasErrors = true;
				}
			}
			else if (strcmp16(block.getElementName(&len), application16.string()) == 0) {
				if (validateAttr(manifestPath, finalResTable, block, RESOURCES_ANDROID_NAMESPACE, "name", classIdentChars, false) != ATTR_OKAY) {
					hasErrors = true;
				}
				if (validateAttr(manifestPath, finalResTable, block, RESOURCES_ANDROID_NAMESPACE, "permission", packageIdentChars, false) != ATTR_OKAY) {
					hasErrors = true;
				}
				if (validateAttr(manifestPath, finalResTable, block, RESOURCES_ANDROID_NAMESPACE, "process", processIdentChars, false) != ATTR_OKAY) {
					hasErrors = true;
				}
				if (validateAttr(manifestPath, finalResTable, block, RESOURCES_ANDROID_NAMESPACE, "taskAffinity", processIdentChars, false) != ATTR_OKAY) {
					hasErrors = true;
				}
			}
			else if (strcmp16(block.getElementName(&len), provider16.string()) == 0) {
				if (validateAttr(manifestPath, finalResTable, block, RESOURCES_ANDROID_NAMESPACE, "name", classIdentChars, true) != ATTR_OKAY) {
					hasErrors = true;
				}
				if (validateAttr(manifestPath, finalResTable, block, RESOURCES_ANDROID_NAMESPACE, "authorities", authoritiesIdentChars, true) != ATTR_OKAY) {
					hasErrors = true;
				}
				if (validateAttr(manifestPath, finalResTable, block, RESOURCES_ANDROID_NAMESPACE, "permission", packageIdentChars, false) != ATTR_OKAY) {
					hasErrors = true;
				}
				if (validateAttr(manifestPath, finalResTable, block, RESOURCES_ANDROID_NAMESPACE, "process", processIdentChars, false) != ATTR_OKAY) {
					hasErrors = true;
				}
			}
			else if (strcmp16(block.getElementName(&len), service16.string()) == 0
				|| strcmp16(block.getElementName(&len), receiver16.string()) == 0
				|| strcmp16(block.getElementName(&len), activity16.string()) == 0) {
				if (validateAttr(manifestPath, finalResTable, block, RESOURCES_ANDROID_NAMESPACE, "name", classIdentChars, true) != ATTR_OKAY) {
					hasErrors = true;
				}
				if (validateAttr(manifestPath, finalResTable, block, RESOURCES_ANDROID_NAMESPACE, "permission", packageIdentChars, false) != ATTR_OKAY) {
					hasErrors = true;
				}
				if (validateAttr(manifestPath, finalResTable, block, RESOURCES_ANDROID_NAMESPACE, "process", processIdentChars, false) != ATTR_OKAY) {
					hasErrors = true;
				}
				if (validateAttr(manifestPath, finalResTable, block, RESOURCES_ANDROID_NAMESPACE, "taskAffinity", processIdentChars, false) != ATTR_OKAY) {
					hasErrors = true;
				}
			}
			else if (strcmp16(block.getElementName(&len), action16.string()) == 0
				|| strcmp16(block.getElementName(&len), category16.string()) == 0) {
				if (validateAttr(manifestPath, finalResTable, block, RESOURCES_ANDROID_NAMESPACE, "name", packageIdentChars, true) != ATTR_OKAY) {
					hasErrors = true;
				}
			}
			else if (strcmp16(block.getElementName(&len), data16.string()) == 0) {
				if (validateAttr(manifestPath, finalResTable, block, RESOURCES_ANDROID_NAMESPACE, "mimeType", typeIdentChars, true) != ATTR_OKAY) {
					hasErrors = true;
				}
				if (validateAttr(manifestPath, finalResTable, block, RESOURCES_ANDROID_NAMESPACE, "scheme", schemeIdentChars, true) != ATTR_OKAY) {
					hasErrors = true;
				}
			}
			else if (strcmp16(block.getElementName(&len), feature_group16.string()) == 0) {
				int depth = 1;
				while ((code = block.next()) != ResXMLTree::END_DOCUMENT
					&& code > ResXMLTree::BAD_DOCUMENT) {
					if (code == ResXMLTree::START_TAG) {
						depth++;
						if (strcmp16(block.getElementName(&len), uses_feature16.string()) == 0) {
							ssize_t idx = block.indexOfAttribute(
								RESOURCES_ANDROID_NAMESPACE, "required");
							if (idx < 0) {
								continue;
							}
							int32_t data = block.getAttributeData(idx);
							if (data == 0) {
								fprintf(stderr, "%s:%d: Tag <uses-feature> can not have android:required=\"false\" when inside a <feature-group> tag.\n", manifestPath.string(), block.getLineNumber());
								hasErrors = true;
							}
						}
					}
					else if (code == ResXMLTree::END_TAG) {
						depth--;
						if (depth == 0) {
							break;
						}
					}
				}
			}
		}
	}
	if (hasErrors) {
		return UNKNOWN_ERROR;
	}
	if (resFile != NULL) {
		// These resources are now considered to be a part of the included     
		// resources, for others to reference.    
		err = assets->addIncludedResources(resFile);
		if (err < NO_ERROR) {
			fprintf(stderr, "ERROR: Unable to parse generated resources, aborting.\n");
			return err;
		}
	}
	return err;
}
```
* 首先从AaptAssets的成员变量mFiles中拿到AndroidManifest.xml路径
* 解析AndroidManifest.xml文件
* 创建一个ResourceTable对象构造中传入AndroidManifest.xml的包名
* 添加被引用的资源包，比如android.jar中那些系统提前编译好的资源文件
* 设置U8编码方式
* 将assets各类资源文件重新收集到resources中KeyedVector<String8, sp<ResourceTypeSet> >*resources = new KeyedVector<String8, sp<ResourceTypeSet> >;
* 定义各种资源文件容器
* 将保存到resources中的各类文件set到我们的容器中
* 判断是不是Overlay链表存在，如果存在则遍历将所有资源进行收集到resources中，如果有这些资源就进行替换当前set到容器中的资源。
* 然后进行drawable，mipmap，layout等等这些文件中R文件的生成当R文件生成
* 由于values特殊性则需要使用compileResourceFile单独进行解析生成R文件
* 以上所有的内容均保存在ResourceTable对象中
* 然后我们就开始分配bag Idtable.assignResourceIds();
* 然后编译xml文件，由于前面收集到了资源，所以后面就可以用
* 将AndroidManifest.xml文件进行flatten检测AndroidManifest.xml文件是否有overlay资源如果有就覆盖
* 编译AndroidManifest.xml文件
* 生成资源符号表，生成资源索引表new AaptFile(String8("resources.arsc")
* ResourceTable::flatten用于生成资源索引表resources.arsc


```cpp
ResourceTable::ResourceTable(Bundle* bundle, const String16& assetsPackage, ResourceTable::PackageType type)
    : mAssetsPackage(assetsPackage)
    , mPackageType(type)
    , mTypeIdOffset(0)
    , mNumLocal(0)
    , mBundle(bundle)
{
    ssize_t packageId = -1;
    switch (mPackageType) {
        case App:
        case AppFeature:
            packageId = 0x7f;
            break;

        case System:
            packageId = 0x01;
            break;

        case SharedLibrary:
            packageId = 0x00;
            break;

        default:
            assert(0);
            break;
    }
    sp<Package> package = new Package(mAssetsPackage, packageId);
    mPackages.add(assetsPackage, package);
    mOrderedPackages.add(package);

    // Every resource table always has one first entry, the bag attributes.
    const SourcePos unknown(String8("????"), 0);
    getType(mAssetsPackage, String16("attr"), unknown);
}
```