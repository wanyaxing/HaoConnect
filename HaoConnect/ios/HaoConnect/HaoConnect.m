//
//  HaoConnect.m
//  HaoxiHttprequest
//
//  Created by lianghuigui on 15/12/3.
//  Copyright © 2015年 lianghuigui. All rights reserved.
//

#import "HaoConnect.h"
#import "HaoUtility.h"
#import "HaoHttpClient.h"

static NSString * Isdebug      = @"0"; //是否打印调试信息
static NSString * Devicetype   = @"4"; //设备类型 1：浏览器设备 2：pc设备 3：Android设备 4：ios设备 5：windows phone设备
static NSString * Requesttime  = @""; //请求时的时间戳，单位：秒
static NSString * Signature    = @""; //接口加密校验

static NSString * Devicetoken = @""; //推送用的设备token
static NSString * Userid      = @""; //当前用户ID，登录后可获得。
static NSString * Logintime   = @""; //登录时间，时间戳，单位：秒，数据来自服务器
static NSString * Checkcode   = @""; //Userid和Logintime组合加密后的产物，用于进行用户信息加密。数据来自服务器


/**
 *  请求加密后的校验串，服务器会使用同样规则加密请求后，比较校验串是否一致，从而防止请求内容被纂改。
 *  取头信息里Clientversion,Devicetype,Requesttime,Devicetoken,Userid,Logintime,Checkcode,Clientinfo,Isdebug  和 表单数据
 *  每个都使用key=value（空则空字符串）格式组合成字符串然后放入同一个数组
 *  再放入请求地址（去除http://和末尾/?#之后的字符）后
 *  并放入私钥字符串后自然排序
 *  连接为字符串后进行MD5加密，获得Signature
 *  将Signature也放入头信息，进行传输。
 */
@implementation HaoConnect


+(void)setIsDebug:(BOOL)isdebug{

    Isdebug = [NSString stringWithFormat:@"%d",isdebug];
}

+ (void)setCurrentUserInfo:(NSString *)userid :(NSString *)loginTime :(NSString *)checkCode{
    
    Userid    = [NSString stringWithFormat:@"%@", userid];
    Logintime = [NSString stringWithFormat:@"%@", loginTime];
    Checkcode = [NSString stringWithFormat:@"%@", checkCode];

    [[NSUserDefaults standardUserDefaults] setObject:userid forKey:@"userid"];
    [[NSUserDefaults standardUserDefaults] setObject:loginTime forKey:@"loginTime"];
    [[NSUserDefaults standardUserDefaults] setObject:checkCode forKey:@"checkCode"];


}
+ (void)setCurrentDeviceToken:(NSString *)deviceToken{

    [[NSUserDefaults standardUserDefaults] setObject:deviceToken forKey:@"deviceToken"];

}

//头信息赋值
+ (NSMutableDictionary *)getCommonHeaderInfo
{
    NSMutableDictionary *commonParams = [[NSMutableDictionary alloc] init];
    
    Requesttime = [NSString stringWithFormat:@"%.0f",[[NSDate date] timeIntervalSince1970]];
    
    if (![Devicetoken isKindOfClass:[NSString class]] || Devicetoken.length == 0)
    {
        Devicetoken = [NSString stringWithFormat:@"%@", [[NSUserDefaults standardUserDefaults] objectForKey:@"deviceToken"]];
    }
    
    if (![Userid isKindOfClass:[NSString class]] || Userid.length == 0)
    {
        Userid = [NSString stringWithFormat:@"%@", [[NSUserDefaults standardUserDefaults] objectForKey:@"userid"]];
    }
    
    if (![Logintime isKindOfClass:[NSString class]] || Logintime.length == 0)
    {
        Logintime = [NSString stringWithFormat:@"%@", [[NSUserDefaults standardUserDefaults] objectForKey:@"loginTime"]];
    }
    
    if (![Checkcode isKindOfClass:[NSString class]] || Checkcode.length == 0)
    {
        Checkcode = [NSString stringWithFormat:@"%@", [[NSUserDefaults standardUserDefaults] objectForKey:@"checkCode"]];
    }

    [commonParams setObject:[HaoConfig getClientInfo]                                           forKey:@"Clientinfo"];
    [commonParams setObject:[HaoConfig getClientVersion]                                        forKey:@"Clientversion"];
    [commonParams setObject:Isdebug                                                             forKey:@"Isdebug"];
    [commonParams setObject:Devicetype                                                          forKey:@"Devicetype"];
    [commonParams setObject:Devicetoken ? [NSString stringWithFormat:@"%@", Devicetoken]  : @"" forKey:@"Devicetoken"];
    [commonParams setObject:Requesttime ? [NSString stringWithFormat:@"%@", Requesttime]  : @"" forKey:@"Requesttime"];
    [commonParams setObject:Userid      ? [NSString stringWithFormat:@"%@", Userid]       : @"" forKey:@"Userid"];
    [commonParams setObject:Logintime   ? [NSString stringWithFormat:@"%@", Logintime]    : @"" forKey:@"Logintime"];
    [commonParams setObject:Checkcode   ? [NSString stringWithFormat:@"%@", Checkcode]    : @"" forKey:@"Checkcode"];

    return commonParams;
}

//头信息加密
+ (NSMutableDictionary * )getSecretHeaders:(NSDictionary *)paramDic urlPrame:(NSString *)urlParam{



    NSMutableArray *array                 = [[NSMutableArray alloc] init];
    NSMutableDictionary *headerDictionary = [NSMutableDictionary dictionaryWithDictionary:[self getCommonHeaderInfo]];

    NSArray *paramKeys                    = [headerDictionary allKeys];
    for (NSString *key in paramKeys) {
        [array addObject:[NSString stringWithFormat:@"%@=%@",key,[headerDictionary objectForKey:key]]];
    }

    for (NSString * key in [paramDic allKeys]) {
        [array addObject:[NSString stringWithFormat:@"%@=%@",key,[paramDic objectForKey:key]]];
    }
    [array addObject:[NSString stringWithFormat:@"link=%@/%@",[HaoConfig getApiHost],urlParam]];

    [array addObject:[NSString stringWithFormat:@"%@",[HaoConfig getSecretHax]]];

    NSArray *resultArray                  = [array sortedArrayUsingSelector:@selector(compare:)];
    NSMutableString *secretString         = [NSMutableString string];

    for (NSString *str in resultArray) {
        [secretString appendString:str];
    }

    [headerDictionary setObject:[HaoUtility md5:secretString] forKey:@"Signature"];
    NSLog(@"headerDictionary=%@",headerDictionary);
    return headerDictionary;
}

+ (MKNetworkOperation *)loadContent:(NSString *)urlParam
            params:(NSMutableDictionary *)params
            method:(NSString *)method
      onCompletion:(void (^)(NSData *responseData))completionBlock
           onError:(MKNKErrorBlock)errorBlock

{

    NSDictionary * headers=[self getSecretHeaders:params urlPrame:urlParam];
    NSString * hostName=[NSString stringWithFormat:@"%@/%@",[HaoConfig getApiHost],urlParam];
    MKNetworkOperation * op = [HaoHttpClient loadContent:hostName params:params method:method headers:headers onCompletion:^(NSData *responseData) {
        completionBlock(responseData);
    } onError:^(NSError *error) {
        errorBlock(error);
    }];

    return op;
}

+ (MKNetworkOperation *)request:(NSString *)urlParam
                         params:(NSMutableDictionary *)params
                     httpMethod:(NSString *)method
                   onCompletion:(void (^)(HaoResult *result))completionBlock
                        onError:(void (^)(HaoResult *errorResult))errorBlock
{
    return [self requestWithAction:RequestActionNull
                        actionPath:nil
                          urlParam:urlParam
                            params:params
                        httpMethod:method
                      onCompletion:completionBlock
                           onError:errorBlock];
}

+ (MKNetworkOperation *)requestFindWithPath:(NSString *)path
                                   urlParam:(NSString *)urlParam
                                     params:(NSMutableDictionary *)params
                                 httpMethod:(NSString *)method
                               onCompletion:(void (^)(HaoResult *result))completionBlock
                                    onError:(void (^)(HaoResult *errorResult))errorBlock
{
    path = [path isKindOfClass:[NSString class]] && path.length != 0 ? path : nil;
    RequestAction action = path ? RequestActionFind : RequestActionNull;
    
    return [self requestWithAction:action
                        actionPath:path
                          urlParam:urlParam
                            params:params
                        httpMethod:method
                      onCompletion:completionBlock
                           onError:errorBlock];
}

+ (MKNetworkOperation *)requestSearchWithPath:(NSString *)path
                                     urlParam:(NSString *)urlParam
                                       params:(NSMutableDictionary *)params
                                   httpMethod:(NSString *)method
                                 onCompletion:(void (^)(HaoResult *result))completionBlock
                                      onError:(void (^)(HaoResult *errorResult))errorBlock
{
    path = [path isKindOfClass:[NSString class]] && path.length != 0 ? path : nil;
    RequestAction action = path ? RequestActionSearch : RequestActionNull;
    
    return [self requestWithAction:action
                        actionPath:path
                          urlParam:urlParam
                            params:params
                        httpMethod:method
                      onCompletion:completionBlock
                           onError:errorBlock];
}

+ (MKNetworkOperation *)requestWithAction:(RequestAction)action
                               actionPath:(NSString *)actionPath
                                 urlParam:(NSString *)urlParam
                                   params:(NSMutableDictionary *)params
                               httpMethod:(NSString *)method
                             onCompletion:(void (^)(HaoResult *result))completionBlock
                                  onError:(void (^)(HaoResult *errorResult))errorBlock
{
    if (action == RequestActionFind)
    {
        params = [NSMutableDictionary dictionaryWithDictionary:params];
        NSDictionary *findDic = @{@"find_paths" : actionPath};
        [params setValuesForKeysWithDictionary:findDic];
    }
    
    if (action == RequestActionSearch)
    {
        params = [NSMutableDictionary dictionaryWithDictionary:params];
        NSDictionary *searchDic = @{@"search_paths" : actionPath};
        [params setValuesForKeysWithDictionary:searchDic];
    }
    
    MKNetworkOperation *op = [self loadContent:urlParam params:params method:method onCompletion:^(NSData *responseData)
    {
        @try {
            NSError *err = nil;
            NSDictionary *jsonDic = [NSJSONSerialization JSONObjectWithData:responseData options:NSJSONReadingAllowFragments error:&err];
            NSLog(@"jsonDic = %@", jsonDic);
            
            HaoResult *resultData = nil;
            
            if (jsonDic && [jsonDic isKindOfClass:[NSDictionary class]])
            {
                if ([[jsonDic objectForKey:@"errorCode"] isKindOfClass:[NSNull class]])
                {
                    NSMutableDictionary *tempJsonDic = jsonDic.mutableCopy;
                    [tempJsonDic setObject:@"-1" forKey:@"errorCode"];
                    [tempJsonDic setObject:@"数据异常" forKey:@"errorStr"];
                    jsonDic = tempJsonDic;
                    
                    NSString *data = [[NSString alloc] initWithData:responseData encoding:NSUTF8StringEncoding];
                    NSLog(@"[数据异常]data ============ %@", data);
                }
                
                resultData = [HaoResult instanceModel:[jsonDic objectForKey:@"results"]
                                            errorCode:[[jsonDic objectForKey:@"errorCode"] integerValue]
                                             errorStr:[jsonDic objectForKey:@"errorStr"]
                                            extraInfo:[jsonDic objectForKey:@"extraInfo"]];
            }
            else
            {
                resultData = [HaoResult instanceModel:nil
                                            errorCode:-1
                                             errorStr:@"JSON解析失败"
                                            extraInfo:nil];
            }
            
            if (resultData && [resultData isResultsOK])
            {
                if (completionBlock)
                {
                    completionBlock(resultData);
                }
                else
                {
                    NSLog(@"What are you 弄啥咧!!!!!!!!!!");
                }
            }
            else
            {
                NSLog(@"errorCode==%@",[jsonDic objectForKey:@"errorStr"]);
                
                [[self class] errorWithErrorBlock:errorBlock result:resultData];
            }
        }
        @catch (NSException *exception)
        {
            NSLog(@"responseData=%@",[[NSString alloc] initWithData:responseData encoding:NSUTF8StringEncoding]);
            HaoResult *errorResult = [HaoResult instanceModel:nil errorCode:-1 errorStr:@"数据异常" extraInfo:nil];
            [[self class] errorWithErrorBlock:errorBlock result:errorResult];
        }
        @finally
        {
            
        }
        
    } onError:^(NSError *error) {
        NSLog(@"error=%@",error);
        HaoResult *errorResult = [HaoResult instanceModel:nil errorCode:-1 errorStr:@"网络请求失败" extraInfo:nil];
        [[self class] errorWithErrorBlock:errorBlock result:errorResult];
    }];

    return op;
}

+ (void)errorWithErrorBlock:(void (^)(HaoResult *errorResult))errorBlock
                     result:(HaoResult *)result
{
    if (errorBlock)
    {
        errorBlock(result);
    }
    else
    {
        UIAlertView * alertView = [[UIAlertView alloc] initWithTitle:@"温馨提示" message:result.errorStr delegate:nil cancelButtonTitle:@"确定" otherButtonTitles:nil, nil];
        [alertView show];
    }
}

+ (MKNetworkOperation *)loadJson:(NSString *)urlParam
         params:(NSMutableDictionary *)params
         Method:(NSString *)method
   onCompletion:(void (^)(NSDictionary *responseData))completionBlock
        onError:(MKNKErrorBlock)errorBlock
{
    MKNetworkOperation * op = [self loadContent:urlParam params:params method:method onCompletion:^(NSData *responseData) {
        @try {
    NSError *err                          = nil;
            NSDictionary * jsonDic=[NSJSONSerialization JSONObjectWithData:responseData options:NSJSONReadingAllowFragments error:&err];
    NSLog(@"jsonDic                       = %@", jsonDic);
            NSLog(@"errorCode==%@",[jsonDic objectForKey:@"errorStr"]);
            completionBlock(jsonDic);
        }
        @catch (NSException *exception) {
        }
        @finally {

        }

    } onError:^(NSError *error) {
        errorBlock(error);
    }];

    return op;
}

+ (MKNetworkOperation *)upLoadImage:(NSString *)urlParam
            params:(NSMutableDictionary *)params
           imgData:(NSData *)imgData
            Method:(NSString *)method
      onCompletion:(void (^)(HaoResult *result))completionBlock
           onError:(void (^)(HaoResult *errorResult))errorBlock{
    NSDictionary * headers=[self getSecretHeaders:params urlPrame:nil];

    MKNetworkOperation * op = [HaoHttpClient uploadImage:urlParam params:params imageDatas:imgData Method:method headers:headers onCompletion:^(NSData *responseData) {
        @try {
    NSError *err                          = nil;
            NSDictionary * jsonDic=[NSJSONSerialization JSONObjectWithData:responseData options:NSJSONReadingAllowFragments error:&err];
    NSLog(@"jsonDic                       = %@", jsonDic);
            HaoResult * resultData=[HaoResult instanceModel:[jsonDic objectForKey:@"results"] errorCode:[[jsonDic objectForKey:@"errorCode"] integerValue] errorStr:[jsonDic objectForKey:@"errorStr"] extraInfo:[jsonDic objectForKey:@"extraInfo"]];
            if ([resultData isResultsOK]) {
                completionBlock(resultData);
            }else{
                NSLog(@"errorCode==%@",[jsonDic objectForKey:@"errorStr"]);
                errorBlock(resultData);
            }

        }
        @catch (NSException *exception) {
            HaoResult * errorResult=[HaoResult instanceModel:nil errorCode:-1 errorStr:@"JSON解析失败" extraInfo:nil];
            errorBlock(errorResult);

        }
        @finally {

        }


    } onError:^(NSError *error) {
        HaoResult * errorResult=[HaoResult instanceModel:nil errorCode:-1 errorStr:@"JSON解析失败" extraInfo:nil];
        errorBlock(errorResult);
    }];
    return op;

}
+ (void)canelRequest:(NSString *)urlParam{
    [HaoHttpClient canelRequest:urlParam];
}
+ (void)canelAllRequest{
    [HaoHttpClient canelAllRequest:(NSString *)[HaoConfig getApiHost]];
}
@end
