//
//  downloader.m
//  downloader
//
//  Created by jenkins on 2022/7/27.
//

#import <Foundation/Foundation.h>
#import "downloader.h"

@implementation RODownloader

- (NSMutableArray<HWDownloadModel *> *)dataSource
{
    if (!_dataSource) {
        _dataSource = [NSMutableArray array];
    }
    
    return _dataSource;
}

//应用创建
- (BOOL) onApplicationFinishLaunching : (UIApplication *) application withOptions : (NSDictionary *) options
{
    NSLog(@"RODownloader onApplicationFinishLaunching");

    // 初始化下载最大并发数为5，不允许蜂窝网络下载
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    [defaults setInteger:3 forKey:HWDownloadMaxConcurrentCountKey];
    [defaults setBool:NO forKey:HWDownloadAllowsCellularAccessKey];
    
    // 开启网络监听
    [[HWNetworkReachabilityManager shareManager] monitorNetworkStatus];
    
    // 初始化下载单例，若之前程序杀死时有正在下的任务，会自动恢复下载
    [HWDownloadManager shareManager];
    
    // 获取已缓存数据
    NSArray *cacheData = [[HWDataBaseManager shareManager] getAllCacheData];
    for (HWDownloadModel *downloadModel in cacheData) {
        [self.dataSource addObject:downloadModel];
    }
    NSLog(@"RODownloader count %lu", self.dataSource.count);
    
    // 添加事件监听
    [self addNotification];
    
    return NO;
}

//应用处于后台
- (void) onHandleEventsForBackgroundURLSession : (UIApplication *)application identifier:(NSString *)identifier completionHandler:(void (^)(void))completionHandler
{
    NSLog(@"RODownloader onHandleEventsForBackgroundURLSession: %@", identifier);
    _backgroundSessionCompletionHandler = completionHandler;
}

//应用将要关闭
- (void) onApplicationWillTerminate : (UIApplication *) application
{
    NSLog(@"RODownloader onApplicationWillTerminate");
    [[NSNotificationCenter defaultCenter] removeObserver:self];
}
// 提供给unity调用的接口
- (void) handleUnityMessage:(NSString *)name withArgs:(NSString *)args
{
    NSLog(@"RODownloader handleUnityData %@ %@", name, args);
    if ([name isEqualToString:@"SetUseCdn"]){
        [self SetUseCdn:args];
    }
    else if ([name isEqualToString:@"ClearLocalCache"]){
        [self ClearLocalCache];
    }
    else if ([name isEqualToString:@"StartDownloadPkg"]){
        [self StartDownloadPkg];
    }
    else if ([name isEqualToString:@"PauseDownloadPkg"]){
        [self PauseDownloadPkg];
    }
    else if ([name isEqualToString:@"ContinueDownloadPkg"]){
        [self ContinueDownloadPkg];
    }
    else if ([name isEqualToString:@"EnableDownloadWithoutWifi"]){
        [self EnableDownloadWithoutWifi];
    }
}
// 提供给unity调用的接口，带参数返回值
- (NSString*) handleUnityGetArgs:(NSString *)name withArgs:(NSString *)args
{
    NSLog(@"RODownloader handleUnityData %@ %@", name, args);
    return @"";
}
- (void) fileNotExistCreateDir:(NSString *)filePath
{
    NSLog(@"RODownloader fileNotExistCreateDir %@", filePath);
    //判断文件夹是否存在，不存在就创建
    NSString *dir = [filePath substringToIndex:[filePath rangeOfString:@"/" options:NSBackwardsSearch].location];
    NSFileManager * fileManager =[NSFileManager defaultManager];
    BOOL isDic = NO;
    if(![fileManager fileExistsAtPath:dir isDirectory:&isDic])
    {
        NSLog(@"RODownloader createDirectoryAtPath %@, isDic = %@", dir, isDic ? @"YES" : @"NO");
        [fileManager createDirectoryAtPath:dir withIntermediateDirectories:YES attributes:nil error:nil];
    }
}
- (void) SetUseCdn:(NSString *)params
{
    NSLog(@"RODownloader SetUseCdn params: %@",params);
    NSData* jsonData = [params dataUsingEncoding:NSUTF8StringEncoding];
    NSMutableDictionary* jsonDic = [NSJSONSerialization JSONObjectWithData:jsonData options:NSJSONReadingMutableContainers error:nil];
    NSString* url = [jsonDic objectForKey:@"url"];
    NSString* fileName = [jsonDic objectForKey:@"fileName"];
    NSString* localPath = [jsonDic objectForKey:@"localPath"];
    bool flag = false;
    for(NSInteger i = 0; i < self.dataSource.count; i++)
    {
        if([self.dataSource[i].url isEqualToString:url])
        {
            NSLog(@"RODownloader has cache");
            flag = true;
            break;
        }
    }
    if (!flag)
    {
        NSLog(@"RODownloader add new data");
        //判断文件夹是否存在，不存在就创建
        [self fileNotExistCreateDir:localPath];
        HWDownloadModel *data = [HWDownloadModel alloc];
        data.url = url;
        data.fileName = fileName;
        data.localPath = localPath;
        data.vid = [NSString stringWithFormat:@"%lu", self.dataSource.count];
        [self.dataSource addObject:data];
    }
}
// 清空本地缓存
- (void)ClearLocalCache
{
    NSLog(@"RODownloader ClearLocalCache");
    dispatch_queue_t queue = dispatch_get_global_queue(0, 0);
    dispatch_async(queue, ^{
        NSArray *array = [[HWDataBaseManager shareManager] getAllCacheData];
        for (HWDownloadModel *model in array) {
            [[HWDownloadManager shareManager] deleteTaskAndCache:model];
        }
        [self.dataSource removeAllObjects];
        NSLog(@"RODownloader count %lu", self.dataSource.count);
    });
}
- (void) StartDownloadPkg
{
    NSLog(@"RODownloader StartDownloadPkg");
    for(NSInteger i = 0; i < self.dataSource.count; i++)
    {
        HWDownloadModel *data = self.dataSource[i];
        if (data.state == HWDownloadStateDefault || data.state == HWDownloadStatePaused || data.state == HWDownloadStateError) {
            // 点击默认、暂停、失败状态，调用开始下载
            [[HWDownloadManager shareManager] startDownloadTask:data];
        }
    }
}
- (void) PauseDownloadPkg
{
    NSLog(@"RODownloader PauseDownloadPkg");
    for(NSInteger i = 0; i < self.dataSource.count; i++)
    {
        HWDownloadModel *data = self.dataSource[i];
        if (data.state == HWDownloadStateDownloading || data.state == HWDownloadStateWaiting) {
            // 点击正在下载、等待状态，调用暂停下载
            [[HWDownloadManager shareManager] pauseDownloadTask:data];
        }
    }
}
- (void) ContinueDownloadPkg
{
    NSLog(@"RODownloader ContinueDownloadPkg");
    [self StartDownloadPkg];
}
- (void) EnableDownloadWithoutWifi
{
    NSLog(@"RODownloader EnableDownloadWithoutWifi");
    // 保存
    [[NSUserDefaults standardUserDefaults] setBool:YES forKey:HWDownloadAllowsCellularAccessKey];
    // 通知
    [[NSNotificationCenter defaultCenter] postNotificationName:HWDownloadAllowsCellularAccessChangeNotification object:[NSNumber numberWithBool:YES]];
}
// 添加通知
- (void)addNotification
{
    // 进度通知
    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(downLoadProgress:) name:HWDownloadProgressNotification object:nil];
    // 状态改变通知
    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(downLoadStateChange:) name:HWDownloadStateChangeNotification object:nil];
}

#pragma mark - HWDownloadNotification
// 正在下载，进度回调
- (void)downLoadProgress:(NSNotification *)notification
{
    HWDownloadModel *downloadModel = notification.object;
    [self.dataSource enumerateObjectsUsingBlock:^(HWDownloadModel *model, NSUInteger idx, BOOL * _Nonnull stop) {
        if ([model.url isEqualToString:downloadModel.url]) {
            NSLog(@"progress = %f", downloadModel.progress);
            *stop = YES;
            
            NSMutableDictionary *dict = [NSMutableDictionary dictionary];
            [dict setObject:@"Downloading" forKey:@"eventName"];
            NSString *state = [NSString stringWithFormat:@"%ld", downloadModel.state];
            [dict setObject:state forKey:@"state"];
            NSString *totalFileSize = [NSString stringWithFormat:@"%lu", downloadModel.totalFileSize];
            [dict setObject:totalFileSize forKey:@"totalFileSize"];
            NSString *progress = [NSString stringWithFormat:@"%f", downloadModel.progress];
            [dict setObject:progress forKey:@"progress"];;
            [self sendUnityMessage:dict];
        }
    }];
}
// 状态改变
- (void)downLoadStateChange:(NSNotification *)notification
{
    HWDownloadModel *downloadModel = notification.object;
    [self.dataSource enumerateObjectsUsingBlock:^(HWDownloadModel *model, NSUInteger idx, BOOL * _Nonnull stop) {
        if ([model.url isEqualToString:downloadModel.url]) {
            self.dataSource[idx] = downloadModel;
            NSLog(@"fileName = %@ state = %ld progress = %f localPath = %@", downloadModel.fileName, downloadModel.state, downloadModel.progress, downloadModel.localPath);
            *stop = YES;
            
            NSMutableDictionary *dict = [NSMutableDictionary dictionary];
            [dict setObject:@"DownloadStateChange" forKey:@"eventName"];
            NSString *state = [NSString stringWithFormat:@"%ld", downloadModel.state];
            [dict setObject:state forKey:@"state"];
            [dict setObject:downloadModel.localPath forKey:@"localPath"];
            [self sendUnityMessage:dict];
        }
    }];
}
- (void)sendUnityMessage:(NSMutableDictionary *)dict
{
    NSError* error;
    NSData* jsonData = [NSJSONSerialization dataWithJSONObject:dict options:NSJSONWritingPrettyPrinted error:&error];
    if (!jsonData) {
        NSLog(@"JsonData Got an error: %@", error);
    } else {
        NSString* jsonStr = [[NSString alloc] initWithData:jsonData encoding:NSUTF8StringEncoding];
        NSLog(@"jsonStr = %@", jsonStr);
        [ISDK SendUnityMessage:@"PkgDownloaderCallBack" withArgs:jsonStr];
    }
}
@end

