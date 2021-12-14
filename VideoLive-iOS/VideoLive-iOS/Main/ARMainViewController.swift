//
//  ARMainViewController.swift
//  VideoLive-iOS
//
//  Created by 余生丶 on 2021/6/18.
//

import AttributedString
import MJRefresh
import SDWebImage
import SVProgressHUD
import UIKit

private let reuseIdentifier = "VideoLive_CellID"

class RefreshGifHeader: MJRefreshHeader {
    var rotatingImage: UIImageView?
    
    override var state: MJRefreshState {
        didSet {
            switch state {
            case .idle, .pulling:
                rotatingImage?.stopAnimating()
            case .refreshing:
                rotatingImage?.startAnimating()
            default:
                print("")
            }
        }
    }
    
    override func prepare() {
        super.prepare()
        rotatingImage = UIImageView()
        rotatingImage?.image = UIImage(named: "icon_refresh")
        addSubview(rotatingImage!)
        
        let rotationAnim = CABasicAnimation(keyPath: "transform.rotation.z")
        rotationAnim.fromValue = 0
        rotationAnim.toValue = Double.pi * 2
        rotationAnim.repeatCount = MAXFLOAT
        rotationAnim.duration = 1
        rotationAnim.isRemovedOnCompletion = false
        rotatingImage!.layer.add(rotationAnim, forKey: "rotationAnimation")
    }
    
    override func placeSubviews() {
        super.placeSubviews()
        rotatingImage?.frame = CGRect(x: 0, y: 0, width: 40, height: 40)
        rotatingImage?.center = CGPoint(x: mj_w / 2, y: mj_h / 2)
    }
}

class ARMainViewController: UICollectionViewController {
    private var flowLayout: UICollectionViewFlowLayout!
    private var index = 0
    
    var modelArr = [ARAudioRoomListModel]()
    
    lazy var placeholder: UILabel = {
        let label = UILabel()
        label.frame = CGRect(x: (collectionView.width - 188) / 2, y: (collectionView.height - 100) / 2, width: 188, height: 20)
        label.attributed.text = """
        \("暂无其他房间，请", .foreground(UIColor(hexString: "#C0C0CC")), .font(.systemFont(ofSize: 14)))\("创建房间", .foreground(UIColor(hexString: "#294BFF")), .font(.systemFont(ofSize: 14)), .action(createVideoRoom))
        """
        label.isHidden = true
        return label
    }()
    
    lazy var createButton: UIButton = {
        let button = UIButton(type: .custom)
        button.frame = CGRect(x: ARScreenWidth - 65, y: ARScreenHeight - (self.tabBarController?.tabBar.frame.size.height)! - 104, width: 48, height: 48)
        button.addTarget(self, action: #selector(createVideoRoom), for: .touchUpInside)
        button.setBackgroundImage(UIImage(named: "icon_add"), for: .normal)
        return button
    }()
    
    lazy var footerView: MJRefreshAutoGifFooter = {
        let footer = MJRefreshAutoGifFooter(refreshingBlock: {
            [weak self] () -> Void in
            self?.footerRefresh()
        })
        return footer
    }()

    override func viewDidLoad() {
        super.viewDidLoad()
        (UserDefaults.string(forKey: .uid) != nil) ? login() : registered()

        // Uncomment the following line to preserve selection between presentations
        // self.clearsSelectionOnViewWillAppear = false

        // Do any additional setup after loading the view.
        flowLayout = UICollectionViewFlowLayout()
        flowLayout.sectionInset = UIEdgeInsets(top: 15, left: 15, bottom: 0, right: 15)
        flowLayout?.scrollDirection = .vertical
        flowLayout?.minimumLineSpacing = 15
        flowLayout?.minimumInteritemSpacing = 15
        let width = (ARScreenWidth - 45) / 2
        flowLayout?.itemSize = CGSize(width: width, height: width * 1.09)
        collectionView.collectionViewLayout = flowLayout
        collectionView.mj_header = RefreshGifHeader(refreshingBlock: {
            [weak self] () -> Void in
            self?.headerRefresh()
        })
        
        addLogoImage()
        // self.view.addSubview(placeholder)
        view.insertSubview(createButton, at: 99)
        view.addSubview(placeholder)
        
        NotificationCenter.default.addObserver(self, selector: #selector(headerRefresh), name: UIResponder.audioLiveNotificationLoginSucess, object: nil)
    }
    
    @objc func headerRefresh() {
        index = 1
        requestRoomList()
    }
    
    @objc func footerRefresh() {
        index += 1
        requestRoomList()
    }
    
    func requestRoomList() {
        if UserDefaults.string(forKey: .isLogin) == "true" {
            let parameters: NSDictionary = ["pageSize": 10, "pageNum": index]
            ARNetWorkHepler.getResponseData("getVidRoomList", parameters: parameters as? [String: AnyObject], headers: true, success: { [self] result in
                if result["code"] == 0 {
                    (index == 1) ? modelArr.removeAll() : nil
                    let jsonArr = result["data"]["list"].arrayValue
                    for json in jsonArr {
                        self.modelArr.append(ARAudioRoomListModel(jsonData: json))
                    }
                    (result["data"]["haveNext"] == 1) ? (collectionView.mj_footer = footerView) : (collectionView.mj_footer = nil)
                } else if result["code"] == 1054, index == 1 {
                    self.modelArr.removeAll()
                }
                
                placeholder.isHidden = (self.modelArr.count == 0) ? false : true
                collectionView.reloadData()
                collectionView.mj_header?.endRefreshing()
                collectionView.mj_footer?.endRefreshing()
            }) { _ in
                self.collectionView.mj_header?.endRefreshing()
            }
        } else {
            placeholder.isHidden = false
            collectionView.mj_header?.endRefreshing()
            login()
        }
    }
    
    // 加入房间
    func requestJoinRoom(roomId: String) {
        SVProgressHUD.show(UIImage(named: "icon_loading")!, status: "加载中")
        let parameters: NSDictionary = ["roomId": roomId, "cType": 2, "pkg": Bundle.main.infoDictionary!["CFBundleIdentifier"] as Any]
        ARNetWorkHepler.getResponseData("joinRoom", parameters: parameters as? [String: AnyObject], headers: true) { [weak self] result in
            if result["code"] == 0 {
                SVProgressHUD.dismiss(withDelay: 0.5)
                var infoModel = ARRoomInfoModel(jsonData: result["data"]["room"])
                infoModel.roomId = roomId
                if infoModel.ower?.uid == UserDefaults.string(forKey: .uid) {
                    infoModel.isBroadcaster = true
                }
                infoModel.pullRtmpUrl = result["data"]["pullRtmpUrl"].stringValue
                infoModel.pushUrl = result["data"]["pushUrl"].stringValue
                
                let storyboard = UIStoryboard(name: "Main", bundle: nil)
                let videoVc = storyboard.instantiateViewController(withIdentifier: "VideoLive_Video") as! ARVideoViewController
                infoVideoModel = infoModel
                videoVc.hidesBottomBarWhenPushed = true
                self?.navigationController?.pushViewController(videoVc, animated: true)
            } else if result["code"] == 800 {
                SVProgressHUD.dismiss()
                self?.showToast(text: "房间已解散或不存在", image: "icon_tip_warning")
            }
        } error: { _ in
        }
    }
    
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        navigationController?.navigationBar.setBackgroundImage(createImage(UIColor(hexString: "#F5F6FA")), for: .any, barMetrics: .default)
        navigationController?.navigationBar.isTranslucent = false
        navigationController?.navigationBar.shadowImage = UIImage()
        hidesBottomBarWhenPushed = false
        extendedLayoutIncludesOpaqueBars = true
        navigationController?.navigationBar.isHidden = false
        
        let titleLabel = UILabel()
        titleLabel.text = "视频连麦直播"
        titleLabel.textColor = UIColor(hexString: "#1A1A1E")
        titleLabel.font = UIFont(name: PingFangBold, size: 18)
        titleLabel.frame = CGRect(x: 0, y: 0, width: 100, height: 50)
        navigationItem.leftBarButtonItem = UIBarButtonItem(customView: titleLabel)
        collectionView.mj_header?.beginRefreshing()
    }
    
    @objc func createVideoRoom() {
        let storyboard = UIStoryboard(name: "Main", bundle: nil)
        let createVc = storyboard.instantiateViewController(withIdentifier: "VideoLive_Create")
        createVc.hidesBottomBarWhenPushed = true
        navigationController?.pushViewController(createVc, animated: true)
    }

    /*
     // MARK: - Navigation

     // In a storyboard-based application, you will often want to do a little preparation before navigation
     override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
         // Get the new view controller using [segue destinationViewController].
         // Pass the selected object to the new view controller.
     }
     */

    // MARK: UICollectionViewDataSource

    override func collectionView(_ collectionView: UICollectionView, numberOfItemsInSection section: Int) -> Int {
        // #warning Incomplete implementation, return the number of items
        return modelArr.count
    }

    override func collectionView(_ collectionView: UICollectionView, cellForItemAt indexPath: IndexPath) -> UICollectionViewCell {
        let collectionViewCell: ARMainViewCell! = (collectionView.dequeueReusableCell(withReuseIdentifier: reuseIdentifier, for: indexPath) as! ARMainViewCell)
        collectionViewCell.listModel = modelArr[indexPath.row]
        return collectionViewCell
    }

    // MARK: UICollectionViewDelegate
    
    override func collectionView(_ collectionView: UICollectionView, didSelectItemAt indexPath: IndexPath) {
        let listModel: ARAudioRoomListModel = modelArr[indexPath.row]
        requestJoinRoom(roomId: listModel.roomId!)
    }
}

class ARMainViewCell: UICollectionViewCell {
    @IBOutlet var backImageView: UIImageView!
    @IBOutlet var roomNameLabel: UILabel!
    @IBOutlet var onlineLabel: UILabel!
    
    var listModel: ARAudioRoomListModel? {
        didSet {
            backImageView.sd_setImage(with: NSURL(string: listModel?.imageUrl ?? "") as URL?, placeholderImage: UIImage(named: "icon_back0"))
            roomNameLabel.text = listModel?.roomName
            onlineLabel.text = String(format: "%d人在看", listModel?.userNum ?? 0)
        }
    }
}
