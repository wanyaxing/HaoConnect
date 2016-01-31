//
//  HaoQiNiu.m
//  HaoConnectRequestDemo
//
//  Created by lianghuigui on 16/1/22.
//  Copyright © 2016年 lianghuigui. All rights reserved.
//

#import "HaoQiNiu.h"
#import "HaoConnect.h"
#import "HaoUtility.h"
static NSMutableDictionary * operationDics;


@implementation HaoQiNiu

+(void) initialize{
    operationDics = [[NSMutableDictionary alloc] init];
}
+(NSMutableDictionary *)operationDics{
    return operationDics;
}
+(void) setCommonParam:(id)key value:(id)value{
    [operationDics setValue:value forKey:key];
}
+(id) commonParam:(id) key{
    return [operationDics objectForKey:key];
}

//上传图片
+ (MKNetworkOperation*) uploadImage:(NSString *)actionUrl
                             params:(NSMutableDictionary *)params
                         imageDatas:(NSData *)imgData
                             Method:(NSString *)method
                       onCompletion:(void (^)(NSData * responseData))completionBlock
                            onError:(MKNKErrorBlock)errorBlock
{
    
    MKNetworkEngine *engine  = [[MKNetworkEngine alloc] initWithHostName:actionUrl customHeaderFields:nil];
    
    MKNetworkOperation *op   = [engine operationWithPath:nil
                                                  params:params
                                              httpMethod:method];
    [op addData:imgData forKey:@"file" mimeType:@"image/jpeg" fileName:[NSString stringWithFormat:@"%.0f.jpg",[[NSDate date] timeIntervalSince1970]]];
    
    [op setFreezable:YES];
    
    
    [op addCompletionHandler:^(MKNetworkOperation* completedOperation) {
        NSData *responseData     = [completedOperation responseData];
        completionBlock(responseData);
    } errorHandler:^(MKNetworkOperation *errorOp, NSError* err){
        errorBlock(err);
    }];
    
    
    
    [engine enqueueOperation:op];
    
    
    
    return op;
    
}

+(NSMutableArray *)upLoadAllPictures:(NSMutableArray *)imageDataArray
            onCompletion:(void (^)(NSDictionary *responseData,NSInteger index))completionBlock
                 onError:(void (^)(NSError * error,NSInteger index))errorBlock
                progress:(void (^)(double progress,NSInteger index))progressBlock

{

    
    NSMutableArray * arrayOperaions = [[NSMutableArray alloc] init];
    NSInteger i=0;
    for (NSData * imgData in imageDataArray) {
       MKNetworkOperation * op  = [self requestUpLoadQiNiu:imgData onCompletion:^(NSDictionary *responseData) {
            completionBlock(responseData,i);
        } onError:^(NSError *error) {
            errorBlock(error,i);
            
        } progress:^(double progress) {
            progressBlock(progress,i);
        }];
        [arrayOperaions addObject:op];
        i++;
    }
    
    
    return arrayOperaions;
    
}


+(MKNetworkOperation *)requestUpLoadQiNiu:(NSData *)imageData
                             onCompletion:(void (^)(NSDictionary *responseData))completionBlock
                                  onError:(MKNKErrorBlock)errorBlock
                                 progress:(void (^)(double progress))progressBlock

{
    
    progressBlock(0);
    NSString * md5Data=[HaoUtility md5FileData:imageData];
    NSString * filesize=[NSString stringWithFormat:@"%ld",(unsigned long)imageData.length];
    
    NSMutableDictionary * exprame=nil;
    exprame=[NSMutableDictionary dictionaryWithObjectsAndKeys:
             md5Data,@"md5",
             filesize,@"filesize",
             @"jpg",@"filetype",
             nil];
    NSString * bundleStr = [NSString stringWithFormat:@"%@%.0f",md5Data,[[NSDate date] timeIntervalSince1970]];
    
   MKNetworkOperation * haoOp=[HaoConnect loadJson:@"qiniu/getUploadTokenForQiniu" params:exprame Method:METHOD_POST onCompletion:^(NSDictionary *responseData) {
        
        int errorCode = [[responseData objectForKey:@"errorCode"] intValue];
        if (errorCode==0) {
            BOOL isFileExistInQiniu=[[[responseData objectForKey:@"results"] objectForKey:@"isFileExistInQiniu"] boolValue];
            if (!isFileExistInQiniu) {
                NSString * token=[[responseData objectForKey:@"results"] objectForKey:@"uploadToken"];
               MKNetworkOperation * upLoadOp = [self requestRealUploadImagToken:token imageDatas:imageData onCompletion:^(NSDictionary *responseData) {
                   completionBlock(responseData);
               } onError:^(NSError *error) {
                   errorBlock(error);
               } progress:^(double progress) {
                   progressBlock(progress);
               }];
                [self setCommonParam:bundleStr value:upLoadOp];
            }else{
                completionBlock(responseData);
                progressBlock(1.0);
            }
            
        }
       completionBlock(responseData);

    } onError:^(NSError *error) {
        errorBlock(error);
    }];
    haoOp.bundleStr = bundleStr;
    [haoOp onUploadProgressChanged:^(double progress) {
        NSLog(@"获取token的：%f",progress);
        progressBlock(progress * 0.2);
    }];
    
    return haoOp;
}


+(MKNetworkOperation *)requestRealUploadImagToken:(NSString *)token
                                       imageDatas:(NSData *)imageData
                                     onCompletion:(void (^)(NSDictionary *responseData))completionBlock
                                          onError:(MKNKErrorBlock)errorBlock
                                         progress:(void (^)(double progress))progressBlock
{
    
    NSMutableDictionary * exprame=nil;
    exprame=[NSMutableDictionary dictionaryWithObjectsAndKeys:
             token,@"token",
             nil];

    MKNetworkOperation * op = [self uploadImage:@"upload.qiniu.com" params:exprame imageDatas:imageData Method:METHOD_POST onCompletion:^(NSData *responseData) {
        NSDictionary * dic = [NSJSONSerialization JSONObjectWithData:responseData options:NSJSONReadingAllowFragments error:nil];
        completionBlock(dic);
        progressBlock(1.0);
    } onError:^(NSError *error) {
        errorBlock(error);
    }];
    [op onUploadProgressChanged:^(double progress) {
        NSLog(@"七牛真正上传的：%f",progress);
        progressBlock(0.2 + progress * 0.79);
    }];

    return op;
}


@end
