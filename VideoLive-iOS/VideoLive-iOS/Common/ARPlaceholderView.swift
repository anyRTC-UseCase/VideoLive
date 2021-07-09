//
//  ARPlaceholderView.swift
//  AudioLive-iOS
//
//  Created by 余生丶 on 2021/2/22.
//

import UIKit

class ARPlaceholderView: UIView {

    // 单例
    static let `default` = ARPlaceholderView()
    
    // 添加的父视图
    fileprivate var superView: UIView? = nil
    // 占位图片名
    fileprivate var placeholderImageName: String? = ""
    
    // 占位图片ImageView
    fileprivate lazy var placeholderImageView: UIImageView = {
        let imgView = UIImageView(frame: CGRect.zero)
        imgView.image = UIImage(named: self.placeholderImageName!)
        return imgView
    }()
    
    // 占位文字
    fileprivate var placeholderTitle: String? = ""
    
    // 占位文字Label
    fileprivate lazy var placeholderLabel: UILabel = {
        let label = UILabel()
        label.text = self.placeholderTitle
        label.textColor = UIColor.gray
        label.font = UIFont(name: "PingFang SC", size: 15)
        label.textAlignment = .center
        label.numberOfLines = 0
        return label
    }()
    
    // 占位背景视图
    fileprivate lazy var backgroundView: UIView = {
        let bView = UIView(frame: CGRect.zero)
        
        return bView
    }()
    
    // 声明闭包
    typealias ClickHandlerBlock = ()->()
    fileprivate var clickHandlerBlock: ClickHandlerBlock?
    
    override init(frame: CGRect) {
        super.init(frame: frame)
        
        self.isUserInteractionEnabled = true
        let tap = UITapGestureRecognizer(target: self, action: #selector(didClickPlaceholderView))
        self.addGestureRecognizer(tap)
    }
    
    required init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
}

extension ARPlaceholderView {
    
    // MARK: 点击空白回调
    @objc func didClickPlaceholderView() {
        if clickHandlerBlock != nil {
            clickHandlerBlock!()
        }
    }
    
    // MARK: 显示
    func showPlaceholderView(_ superView: UIView?, placeholderImageName: String?, placeholderTitle: String?, clickHandlerBlock: @escaping ClickHandlerBlock) {
        
        assert((placeholderImageName?.count != 0 || placeholderImageName == nil), "superView must not be nil.");
        
        superView != nil ? (self.superView = superView):(self.superView = topViewController().view)
        //        self.superView = superView
        //        self.superView = topViewController().view
        self.clickHandlerBlock = clickHandlerBlock
        self.placeholderImageName = placeholderImageName
        self.placeholderTitle = placeholderTitle
        
        
        backgroundColor = UIColor.black
        
        
        superView?.addSubview(self)
        addSubview(backgroundView)
        backgroundView.addSubview(placeholderImageView)
        backgroundView.addSubview(placeholderLabel)
        
    }
    
    // MARK: 隐藏
    func dismissPlaceholderView() {
        removeFromSuperview()
    }
}

// MARK: layoutSubviews
extension ARPlaceholderView {
    
    override func layoutSubviews() {
        super.layoutSubviews()
        
        self.frame = (superView?.bounds)!
        let bW: CGFloat = bounds.size.width * 0.356
        let bH: CGFloat = bW
        let bX: CGFloat = (bounds.size.width-bW)/2
        let bY: CGFloat = (bounds.size.height-bH)/2
        
        // 占位Label高度
        var LH: CGFloat = 45
        (placeholderTitle?.count == 0 || placeholderTitle == nil) ? (LH = 0):(LH = 45)
        
        backgroundView.frame = CGRect(x:
            0, y: bY, width: self.bounds.size.width, height: bH)
        
        placeholderImageView.frame = CGRect(x: bX, y: 0, width: bW, height: backgroundView.bounds.size.height-LH)
        
        placeholderLabel.frame = CGRect(x: 0, y: placeholderImageView.bounds.size.height, width: backgroundView.bounds.size.width, height: LH)
    }
}

// MARK: 获取当前topViewController

extension ARPlaceholderView {
    // 获取当前活动的navigationcontroller
    func navigationViewController() -> UINavigationController?{
        let rootVC = UIApplication.shared.keyWindow?.rootViewController
        
        if (rootVC?.isKind(of: UINavigationController.classForCoder()))! {
            return rootVC as? UINavigationController
        } else if (rootVC?.isKind(of: UITabBarController.classForCoder()))! {
            let selectVC = (rootVC as! UITabBarController).selectedViewController
            if (selectVC?.isKind(of: UINavigationController.classForCoder()))! {
                return selectVC as? UINavigationController
            }
        }
        
        return nil
    }
    
    // 获取当前topViewController
    func topViewController() -> UIViewController! {
        let rootVC = UIApplication.shared.keyWindow?.rootViewController
        
        let nav = navigationViewController()
        if nav == nil {
            return rootVC
        }
        return (nav?.topViewController)
    }
}


