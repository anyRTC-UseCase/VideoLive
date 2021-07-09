//
//  ARNetWorkHepler.swift
//  AudioLive-iOS
//
//  Created by 余生丶 on 2021/2/22.
//

import UIKit
import Alamofire
import SwiftyJSON
import SVProgressHUD

private let requestUrl = "https://arlive.agrtc.cn/arapi/arlive/v1/user/"
//private let requestUrl = "http://192.168.1.115:12680/arapi/arlive/v1/user/"

class ARNetWorkHepler: NSObject {
    class func getResponseData(_ url: String, parameters: [String: AnyObject]? = nil, headers: Bool, success:@escaping(_ result: JSON)-> Void, error:@escaping (_ error: NSError)->Void) {
        UIApplication.shared.isNetworkActivityIndicatorVisible = true
        //"Accept": "application/json","Content-Type": "application/json"
        let resultUrl = requestUrl + url
        let urls = NSURL(string: resultUrl as String)
        var request = URLRequest(url: urls! as URL)
        request.httpMethod = (url == "getMusicList") ? "GET" : "POST"
        request.setValue("application/json", forHTTPHeaderField:"Content-Type")
        if headers {
            guard let token = UserDefaults.string(forKey: .userToken) else {return}
            request.setValue("Bearer " + token, forHTTPHeaderField: "Authorization")
        }
        
        if parameters != nil {
            let data = try! JSONSerialization.data(withJSONObject: parameters!, options: JSONSerialization.WritingOptions.prettyPrinted)

            let json = NSString(data: data, encoding: String.Encoding.utf8.rawValue)
            if let json = json {
                print(json)
            }
            request.httpBody = json!.data(using: String.Encoding.utf8.rawValue)
        }
        
        let alamoRequest = Alamofire.request(request as URLRequestConvertible)
        alamoRequest.validate(statusCode: 200..<300)
        alamoRequest.responseString { response in
            print(response)
            if let jsonData = response.result.value {
                success(JSON(parseJSON: jsonData))
            } else if let er = response.result.error {
                
                let errorResult = er as NSError
                error(errorResult)
                SVProgressHUD.dismiss()
                if errorResult.code == -1004 {
                    SVProgressHUD.show(UIImage(named: "icon_loading")!, status: "当前服务异常")
                } else {
                    SVProgressHUD.show(UIImage(named: "icon_loading")!, status: "请检查当前网络状态")
                }
                SVProgressHUD.dismiss(withDelay: 1)
            }
            UIApplication.shared.isNetworkActivityIndicatorVisible = false
        }
    }
}
