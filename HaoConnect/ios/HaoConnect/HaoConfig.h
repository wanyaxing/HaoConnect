//
//  HaoConfig.h
//  HaoxiHttprequest
//
//  Created by lianghuigui on 15/11/29.
//  Copyright © 2015年 lianghuigui. All rights reserved.
//

#import <Foundation/Foundation.h>

static NSString * HAOCONNECT_CLIENTINFO          =@"haoFrame-client";//客户端信息
static NSString * HAOCONNECT_SECRET_HAX          =@"secret=apijfwa194207o0dmxvjc";//加密秘钥  apijfwa194207o0dmxvjc
static NSString * HAOCONNECT_APIHOST             =@"api-haoframe.haoxitech.com";//接口域名，建议根据正式服或测试服环境分别赋值

@interface HaoConfig : NSObject
@end
