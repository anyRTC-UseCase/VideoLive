//
//  ARVideoView.swift
//  VideoLive-iOS
//
//  Created by 余生丶 on 2021/6/21.
//

import UIKit

class ARVideoView: UIView {

    @IBOutlet weak var renderView: UIView!
    @IBOutlet weak var placeholderView: UIView!
    @IBOutlet weak var headImageView: UIImageView!
    @IBOutlet weak var audioImageView: UIImageView!
    @IBOutlet weak var nameLabel: UILabel!
    
    fileprivate var borderLayer: CAShapeLayer?
    
    var uid: String?
    var userName: String? {
        didSet {
            nameLabel.text = userName
        }
    }
    var headUrl: String? {
        didSet {
            headImageView.sd_setImage(with: NSURL(string: headUrl!) as URL?, placeholderImage: UIImage(named: "icon_head"))
        }
    }
    
    class func videoView(uid: String?) -> ARVideoView {
        
        let video = Bundle.main.loadNibNamed("ARVideoView", owner: nil, options: nil)![0] as! ARVideoView
        video.uid = uid
        return video
    }
    
    override var frame: CGRect {
        didSet {
            if (headImageView != nil) {
                headImageView.layer.cornerRadius = frame.width * 0.3/2
            }
            
            if frame.width == ARScreenWidth {
                borderLayer?.removeFromSuperlayer()
                borderLayer = nil
            } else {
                swiftDrawBoardDottedLine(width: 3, lenth: 1, space: 0, cornerRadius: 0, color: UIColor.white)
            }
        }
    }
    
    func swiftDrawBoardDottedLine(width: CGFloat, lenth: CGFloat, space: CGFloat, cornerRadius: CGFloat, color:UIColor) {
        
        borderLayer?.removeFromSuperlayer()
        borderLayer = nil
        
        self.layer.cornerRadius = cornerRadius
        borderLayer =  CAShapeLayer()
        borderLayer?.bounds = self.bounds
        borderLayer?.position = CGPoint(x: self.bounds.midX, y: self.bounds.midY);
        borderLayer?.path = UIBezierPath(roundedRect: borderLayer!.bounds, cornerRadius: cornerRadius).cgPath
        borderLayer?.lineWidth = width / UIScreen.main.scale
        
        borderLayer?.lineDashPattern = [lenth,space] as [NSNumber]
        borderLayer?.lineDashPhase = 0.1;
        
        borderLayer?.fillColor = UIColor.clear.cgColor
        borderLayer?.strokeColor = color.cgColor
        self.layer.addSublayer(borderLayer!)
    }
}
