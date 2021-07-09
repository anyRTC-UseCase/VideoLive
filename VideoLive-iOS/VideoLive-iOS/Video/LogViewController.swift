//
//  LogViewController.swift
//  AudioLive-iOS
//
//  Created by 余生丶 on 2021/3/9.
//

import UIKit
import AttributedString

struct ARLogModel {
    var userName: String?
    var uid: String?
    var text: String?
}

class LogCell: UITableViewCell {
    @IBOutlet weak var contentLabel: UILabel!
    @IBOutlet weak var colorView: UIView!

    override func awakeFromNib() {
        super.awakeFromNib()
        colorView.layer.cornerRadius = 12.25
    }
    
    func update(logModel: ARLogModel) {
        var userName = logModel.userName
        (userName == nil) ? userName = "" : nil
        if logModel.uid == UserDefaults.string(forKey: .uid) {
            contentLabel.attributed.text = """
            \(logModel.userName ?? "", .foreground(UIColor(hexString: "#FFBB8D"))) \(logModel.text ?? "", .foreground(UIColor.white))
            """
        } else {
            contentLabel.attributed.text = """
            \(logModel.userName ?? "", .foreground(UIColor(hexString: "#8DAEFF"))) \(logModel.text ?? "", .foreground(UIColor.white))
            """
        }
    }
}

class LogViewController: UITableViewController {

    private lazy var list = [ARLogModel]()
    
    override func viewDidLoad() {
        super.viewDidLoad()
        tableView.rowHeight = UITableView.automaticDimension
        tableView.estimatedRowHeight = 44
    }
    
    override func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return list.count
    }
    
    override func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCell(withIdentifier: "LogCell", for: indexPath) as! LogCell
        cell.update(logModel: list[indexPath.row])
        return cell
    }
}

extension LogViewController {
    func log(logModel: ARLogModel) {
        DispatchQueue.main.asyncAfter(deadline: DispatchTime.now() + 0.25) {
            self.list.append(logModel)
            let index = IndexPath(row: self.list.count - 1, section: 0)
            self.tableView.insertRows(at: [index], with: .automatic)
            self.tableView.scrollToRow(at: index, at: .middle, animated: false)
        }
    }
}
