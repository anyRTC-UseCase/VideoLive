//
//  AREditViewController.swift
//  VideoLive-iOS
//
//  Created by 余生丶 on 2021/6/21.
//

import UIKit

class AREditViewController: UIViewController {
    @IBOutlet var nameTextField: UITextField!
    @IBOutlet var numLabel: UILabel!
    
    var rightButton: UIButton!

    override func viewDidLoad() {
        super.viewDidLoad()

        // Do any additional setup after loading the view.
        
        initializeUI()
    }
    
    func initializeUI() {
        navigationItem.leftBarButtonItem = createBarButtonItem(title: "设置昵称")
        
        rightButton = UIButton(type: .custom)
        rightButton.setTitle("保存", for: .normal)
        rightButton.titleLabel?.font = UIFont(name: PingFangBold, size: 14)
        rightButton.setTitleColor(UIColor(hexString: "#C0C0CC"), for: .normal)
        rightButton.addTarget(self, action: #selector(saveNickname), for: .touchUpInside)
        navigationItem.rightBarButtonItem = UIBarButtonItem(customView: rightButton)
        
        nameTextField.becomeFirstResponder()
        nameTextField.addTarget(self, action: #selector(textFieldValueChange), for: .editingChanged)
    }
    
    @objc func textFieldValueChange() {
        let nickName = nameTextField.text
        if nickName?.count ?? 0 > 16 {
            nameTextField.text = String((nickName?.prefix(16))!)
        }
        numLabel.text = "\(16 - Int(nameTextField.text?.count ?? 0))"
        (nickName?.count ?? 0 > 0) ? (rightButton.alpha = 1.0) : (rightButton.alpha = 0.5)
        rightButton.isEnabled = nickName?.count ?? 0 > 0
        rightButton.setTitleColor(rightButton.isEnabled ? UIColor(hexString: "#1B5DFF") : UIColor(hexString: "#C0C0CC"), for: .normal)
    }
    
    @objc func saveNickname() {
        let nickName = nameTextField.text
        if Int(nickName?.count ?? 0) > 0, !stringAllIsEmpty(string: nickName ?? "") {
            if nickName != UserDefaults.string(forKey: .userName) {
                UserDefaults.set(value: nickName!, forKey: .userName)
                // 修改昵称
                let parameters: NSDictionary = ["userName": nickName as Any]
                ARNetWorkHepler.getResponseData("updateUserName", parameters: parameters as? [String: AnyObject], headers: true, success: { [self] _ in
                    NotificationCenter.default.post(name: UIResponder.audioLiveNotificationModifySucess, object: self, userInfo: nil)
                    popBack()
                }) { error in
                    print(error)
                }
            } else {
                popBack()
            }
        } else {
            UIAlertController.showAlert(in: self, withTitle: "提示", message: "昵称不能为空", cancelButtonTitle: nil, destructiveButtonTitle: nil, otherButtonTitles: ["确定"]) { _, _, index in
                print("\(index)")
            }
        }
    }
    
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        removeLogoImage()
    }
    
    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        addLogoImage()
    }
    
    override func touchesBegan(_ touches: Set<UITouch>, with event: UIEvent?) {
        view.endEditing(true)
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
