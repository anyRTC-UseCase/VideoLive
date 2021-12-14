//
//  ARBaseNavigationController.swift
//  VideoLive-iOS
//
//  Created by 余生丶 on 2021/06/18.
//

import UIKit

class ARBaseNavigationController: UINavigationController {
    override func viewDidLoad() {
        super.viewDidLoad()

        // Do any additional setup after loading the view.
        if #available(iOS 15.0, *) {
            let appearance = UINavigationBarAppearance()
            appearance.configureWithTransparentBackground()
            // appearance.configureWithOpaqueBackground()
            appearance.backgroundColor = UIColor.white
            navigationBar.standardAppearance = appearance
            navigationBar.scrollEdgeAppearance = navigationBar.standardAppearance
        } else {
            // Fallback on earlier versions
        }
    }

    override var childForStatusBarStyle: UIViewController? {
        return topViewController
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
