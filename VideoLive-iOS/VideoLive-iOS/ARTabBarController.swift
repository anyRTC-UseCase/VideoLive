//
//  ARTabBarController.swift
//  VideoLive-iOS
//
//  Created by 余生丶 on 2021/6/18.
//

import UIKit

let PingFang = "PingFang SC"
let PingFangBold = "PingFangSC-Semibold"

class ARTabBarController: UITabBarController {
    override func viewDidLoad() {
        super.viewDidLoad()

        // Do any additional setup after loading the view.
        if #available(iOS 13.0, *) {
            let appearance = UITabBarAppearance()
            appearance.stackedLayoutAppearance.normal.titlePositionAdjustment = UIOffset(horizontal: 0, vertical: -10)
            appearance.stackedLayoutAppearance.selected.titlePositionAdjustment = UIOffset(horizontal: 0, vertical: -10)

            appearance.stackedLayoutAppearance.normal.titleTextAttributes = [
                NSAttributedString.Key.font: UIFont(name: PingFangBold, size: 14) as Any,
                NSAttributedString.Key.foregroundColor: UIColor(hexString: "#18191D")
            ]
            appearance.stackedLayoutAppearance.selected.titleTextAttributes = [
                NSAttributedString.Key.font: UIFont(name: PingFangBold, size: 14) as Any,
                NSAttributedString.Key.foregroundColor: UIColor(hexString: "#3150FF")
            ]

            self.tabBar.standardAppearance = appearance
        } else {
            // Fallback on earlier versions

            UITabBarItem.appearance().setTitleTextAttributes([NSAttributedString.Key.font: UIFont(name: PingFangBold, size: 14) as Any, NSAttributedString.Key.foregroundColor: UIColor(hexString: "#18191D")], for: .normal)
            UITabBarItem.appearance().setTitleTextAttributes([NSAttributedString.Key.font: UIFont(name: PingFangBold, size: 14) as Any, NSAttributedString.Key.foregroundColor: UIColor(hexString: "#3150FF")], for: .selected)
            UITabBarItem.appearance().titlePositionAdjustment = UIOffset(horizontal: 0, vertical: -10)
        }
    }

    override func tabBar(_ tabBar: UITabBar, didSelect item: UITabBarItem) {
        if item.title == "视频连麦" {
            // refresh
        }
    }

    /*
     // MARK: - Navigation

     // In a storyboard-based application, you will often want to do a little preparation before navigation
     override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
         // Get the new view controller using segue.destination.
         // Pass the selected object to the new view controller.
     }
     */
}
