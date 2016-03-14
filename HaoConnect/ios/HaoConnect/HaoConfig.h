//
//  HaoConfig.h
//  HaoxiHttprequest
//
//  Created by lianghuigui on 15/11/29.
//  Copyright © 2015年 lianghuigui. All rights reserved.
//

#import <Foundation/Foundation.h>


@interface HaoConfig : NSObject
+(NSString *)getClientInfo;
+(NSString *)getSecretHax;
+(NSString *)getApiHost;
+(NSString *)getClientVersion;
@end
