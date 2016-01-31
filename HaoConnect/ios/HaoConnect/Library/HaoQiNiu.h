//
//  HaoQiNiu.h
//  HaoConnectRequestDemo
//
//  Created by lianghuigui on 16/1/22.
//  Copyright © 2016年 lianghuigui. All rights reserved.
//
#import "MKNetworkKit.h"
#import <Foundation/Foundation.h>


@interface HaoQiNiu : NSObject
+(NSMutableDictionary *)operationDics;
+(void) setCommonParam:(id)key value:(id)value;
+(id) commonParam:(id) key;
+(NSMutableArray *)upLoadAllPictures:(NSMutableArray *)imageDataArray
                        onCompletion:(void (^)(NSDictionary *responseData,NSInteger index))completionBlock
                             onError:(void (^)(NSError * error,NSInteger index))errorBlock
                            progress:(void (^)(double progress,NSInteger index))progressBlock;
+(MKNetworkOperation *)requestUpLoadQiNiu:(NSData *)imageData
                             onCompletion:(void (^)(NSDictionary *responseData))completionBlock
                                  onError:(MKNKErrorBlock)errorBlock
                                 progress:(void (^)(double progress))progressBlock;
@end
