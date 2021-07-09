//
//  ARBaseViewController.swift
//  AudioLive-iOS
//
//  Created by 余生丶 on 2021/2/24.
//

import UIKit

class ARBaseViewController: UIViewController {
    
    var keyBoardView: UIView!
    var chatTextField: UITextField!
    var confirmButton: UIButton!

    override func viewDidLoad() {
        super.viewDidLoad()

        // Do any additional setup after loading the view.
        self.view.addSubview(getInputAccessoryView())
        NotificationCenter.default.addObserver(self,selector:#selector(keyboardChange(notify:)), name: UIResponder.keyboardWillShowNotification, object: nil)
        NotificationCenter.default.addObserver(self,selector:#selector(keyboardChange(notify:)), name: UIResponder.keyboardWillHideNotification, object: nil)
    }
    
    func getInputAccessoryView() -> UIView {
        keyBoardView = UIView.init(frame: CGRect.init(x: 0, y: ARScreenHeight, width: ARScreenWidth, height: 44))
        keyBoardView.backgroundColor = UIColor.init(red: 247/255, green: 247/255, blue: 247/255, alpha: 1)
        
        chatTextField = UITextField.init(frame: CGRect.init(x: 10, y: 6, width: ARScreenWidth - 110, height: 32));
        chatTextField.font = UIFont.systemFont(ofSize: 14)
        chatTextField.layer.masksToBounds = true
        chatTextField.layer.cornerRadius = 5
        chatTextField.returnKeyType = .send
        chatTextField.placeholder = "输入点什么"
        chatTextField.backgroundColor = UIColor.white
        chatTextField.delegate = self
        chatTextField.addTarget(self, action: #selector(chatTextFieldLimit), for: .editingChanged)
        
        keyBoardView.addSubview(chatTextField)
        
        confirmButton = UIButton.init(type: .custom)
        confirmButton.frame = CGRect.init(x: ARScreenWidth - 92, y: 6, width: 79, height: 32)
        confirmButton.setTitleColor(UIColor.white, for: .normal)
        confirmButton.backgroundColor = UIColor(hexString: "#314BFF")
        confirmButton.layer.masksToBounds = true
        confirmButton.titleLabel?.font = UIFont.init(name: PingFangBold, size: 12)
        confirmButton.layer.cornerRadius = 5
        confirmButton.alpha = 0.3
        confirmButton.setTitle("发送", for:.normal)
        confirmButton.addTarget(self, action: #selector(didSendChatTextField), for: .touchUpInside)
        keyBoardView.addSubview(confirmButton)
        return keyBoardView
    }
    
    @objc func chatTextFieldLimit() {
        if chatTextField.text?.count ?? 0 > 128 {
            chatTextField.text = String((chatTextField.text?.prefix(128))!)
        }
        
        if isBlank(text: chatTextField.text) || stringAllIsEmpty(string: chatTextField.text ?? "") {
            confirmButton.alpha = 0.3
        } else {
            confirmButton.alpha = 1.0
        }
    }
    
    @objc public func didSendChatTextField() {
        // 发送消息
    }
    
    @objc func keyboardChange(notify:NSNotification){
        if chatTextField.isFirstResponder {
            //时间
            let duration : Double = notify.userInfo![UIResponder.keyboardAnimationDurationUserInfoKey] as! Double
            if notify.name == UIResponder.keyboardWillShowNotification {
                //键盘高度
                let keyboardY : CGFloat = (notify.userInfo?[UIResponder.keyboardFrameEndUserInfoKey] as! NSValue).cgRectValue.size.height
                let high = UIScreen.main.bounds.size.height - keyboardY - 44
                
                UIView.animate(withDuration: duration) {
                    self.keyBoardView.frame = CGRect(x: 0, y: high, width: ARScreenWidth, height: 44)
                    self.view.layoutIfNeeded()
                }
            } else if notify.name == UIResponder.keyboardWillHideNotification {
                
                UIView.animate(withDuration: duration, animations: {
                    self.keyBoardView.frame = CGRect(x: 0, y: ARScreenHeight, width: ARScreenWidth, height: 44)
                    self.view.layoutIfNeeded()
                })
            }
        }
    }
    
    override func touchesBegan(_ touches: Set<UITouch>, with event: UIEvent?) {
        view.endEditing(true)
    }
    
    deinit {
        NotificationCenter.default.removeObserver(self)
    }
}

extension ARBaseViewController: UITextFieldDelegate {
    func textFieldShouldReturn(_ textField: UITextField) -> Bool {
        didSendChatTextField()
        return true
    }
}


