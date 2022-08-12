//
// Created by 董宸 on 2018/9/22.
// Copyright (c) 2018 ___JOYYOU___. All rights reserved.
//

#import <Foundation/Foundation.h>

@protocol IUnity
-(void)SendUnityMessageWithObject : (NSString *) obj andName : (NSString *) name andArgs : (NSString *) args;
@end
