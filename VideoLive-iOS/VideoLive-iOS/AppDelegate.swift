//
//  AppDelegate.swift
//  VideoLive-iOS
//
//  Created by 余生丶 on 2021/6/18.
//

import UIKit
import SVProgressHUD

@main
class AppDelegate: UIResponder, UIApplicationDelegate {

    var window: UIWindow?

    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?) -> Bool {
        // Override point for customization after application launch.
        SVProgressHUD.setDefaultStyle(.light)
        SVProgressHUD.setDefaultMaskType(.black)
        SVProgressHUD.setShouldTintImages(false)
        SVProgressHUD.setMinimumSize(CGSize.init(width: 120, height: 120))
        
        Thread.sleep(forTimeInterval: 0.5)
        return true
    }

}

