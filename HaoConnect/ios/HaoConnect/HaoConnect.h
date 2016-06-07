//
//  HaoConnect.h
//  HaoxiHttprequest
//
//  Created by lianghuigui on 15/12/3.
//  Copyright © 2015年 lianghuigui. All rights reserved.
//

#import "HaoConfig.h"
#import "HaoResult.h"
#import "MKNetworkKit.h"
#define METHOD_GET @"GET"
#define METHOD_POST @"POST"


typedef NS_OPTIONS(NSInteger, RequestAction)
{
    RequestActionNull   = 1, // 常规请求
    RequestActionFind   = 2, // 查找特定结果
    RequestActionSearch = 3, // 搜索特定结果
};


@interface HaoConnect : NSObject
+(void)setIsDebug:(BOOL)isdebug;
+ (void)setCurrentUserInfo:(NSString *)userid :(NSString *)loginTime :(NSString *)checkCode;
+ (void)setCurrentDeviceToken:(NSString *)deviceToken;

+ (NSMutableDictionary * )getSecretHeaders:(NSDictionary *)paramDic urlPrame:(NSString *)urlParam;

+ (MKNetworkOperation *)loadContent:(NSString *)urlParam
                             params:(NSMutableDictionary *)params
                             method:(NSString *)method
                       onCompletion:(void (^)(NSData *responseData))completionBlock
                            onError:(MKNKErrorBlock)errorBlock;

+ (MKNetworkOperation *)request:(NSString *)urlParam
                         params:(NSMutableDictionary *)params
                     httpMethod:(NSString *)method
                   onCompletion:(void (^)(HaoResult *result))completionBlock
                        onError:(void (^)(HaoResult *errorResult))errorBlock;

+ (MKNetworkOperation *)requestFindWithPath:(NSString *)path
                                   urlParam:(NSString *)urlParam
                                     params:(NSMutableDictionary *)params
                                 httpMethod:(NSString *)method
                               onCompletion:(void (^)(HaoResult *result))completionBlock
                                    onError:(void (^)(HaoResult *errorResult))errorBlock;

+ (MKNetworkOperation *)requestSearchWithPath:(NSString *)path
                                     urlParam:(NSString *)urlParam
                                       params:(NSMutableDictionary *)params
                                   httpMethod:(NSString *)method
                                 onCompletion:(void (^)(HaoResult *result))completionBlock
                                      onError:(void (^)(HaoResult *errorResult))errorBlock;

+ (MKNetworkOperation *)loadJson:(NSString *)urlParam
                          params:(NSMutableDictionary *)params
                          Method:(NSString *)method
                    onCompletion:(void (^)(NSDictionary *responseData))completionBlock
                         onError:(MKNKErrorBlock)errorBlock;

+ (MKNetworkOperation *)upLoadImage:(NSString *)urlParam
                             params:(NSMutableDictionary *)params
                            imgData:(NSData *)imgData
                             Method:(NSString *)method
                       onCompletion:(void (^)(HaoResult *result))completionBlock
                            onError:(void (^)(HaoResult *errorResult))errorBlock;//上传图片

+ (void)canelRequest:(NSString *)urlParam;//取消某个请求
+ (void)canelAllRequest;//取消所有的请求
@end
