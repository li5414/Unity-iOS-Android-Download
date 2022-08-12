//
//  downloader.h
//  downloader
//
//  Created by jenkins on 2022/7/27.
//

#import <Foundation/Foundation.h>
#import <UIKit/UIKit.h>
#import <roioslib/ISDK.h>

#import "HWDownload.h"

//! Project version number for downloader.
FOUNDATION_EXPORT double downloaderVersionNumber;

//! Project version string for downloader.
FOUNDATION_EXPORT const unsigned char downloaderVersionString[];

// In this header, you should import all the public headers of your framework using statements like #import <downloader/PublicHeader.h>

@interface RODownloader : ISDK
{

}

@property (nonatomic, copy) void (^ backgroundSessionCompletionHandler)(void);  // 后台所有下载任务完成回调

@property (nonatomic, strong) NSMutableArray<HWDownloadModel *> *dataSource;

@end
