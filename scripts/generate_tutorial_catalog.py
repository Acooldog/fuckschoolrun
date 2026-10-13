"""Generate the same Chinese control catalogue for the app and FSR backend."""
import argparse
import json
from pathlib import Path
import xml.etree.ElementTree as ET

ANDROID = "{http://schemas.android.com/apk/res/android}"
COMPOSE = {
    "dock.draw": ("绘制页面入口", "draw"),
    "dock.simulation": ("模拟页面入口", "simulation"),
    "dock.routes": ("路线管理入口", "routes"),
    "dock.mine": ("我的页面入口", "mine"),
    "draw.search_open": ("展开地点搜索", "draw"),
    "draw.search_input": ("地点搜索输入框", "draw"),
    "draw.search_submit": ("搜索地点", "draw"),
    "draw.search_result": ("地点搜索结果", "draw"),
    "draw.tools": ("路线工具齿轮", "draw"),
    "draw.map_standard": ("标准地图", "draw"),
    "draw.map_satellite": ("卫星地图", "draw"),
    "draw.location": ("当前位置", "draw"),
    "draw.record_mode": ("录制路线开关", "draw"),
    "draw.record_settings": ("录制设置", "draw"),
    "draw.record": ("开始或停止录制", "draw"),
    "draw.close_route": ("闭合路线", "draw"),
    "draw.save": ("保存路线", "draw"),
    "draw.clear": ("清除重画", "draw"),
    "draw.share": ("分享路线", "draw"),
    "draw.mode": ("切换绘制或编辑模式", "draw"),
    "draw.auto_add": ("自动加点", "draw"),
    "draw.auto_reduce": ("自动减点", "draw"),
    "draw.smooth": ("平滑拐角", "draw"),
    "draw.undo": ("撤销", "draw"),
    "draw.redo": ("重做", "draw"),
    "draw.keep": ("保留选中的路线点", "draw"),
    "draw.delete": ("删除选中的路线点", "draw"),
    "simulation.tools": ("模拟操作齿轮", "simulation"),
    "simulation.start": ("开始或停止模拟", "simulation"),
    "simulation.route": ("选择模拟路线", "simulation"),
    "simulation.real_run": ("跑步联动", "simulation"),
    "simulation.nfc": ("NFC 联动", "simulation"),
    "mine.login": ("账号登录入口", "mine"),
    "mine.settings": ("应用设置入口", "mine"),
    "mine.update": ("版本更新入口", "mine"),
    "mine.tutorial": ("重新查看教程", "mine"),
}
UNSAFE = {"draw.clear", "draw.delete", "draw.record", "simulation.start", "simulation.real_run",
          "draw.search_input", "draw.search_result", "mine.tutorial"}
UNSAFE_VIEW_TOKENS = (
    "delete", "clear", "remove", "logout", "submit", "save", "mock", "download",
    "upload", "record", "run", "import",
)


def auto_click_allowed(tag: str, control_id: str) -> bool:
    if not tag.endswith(("Button", "Switch", "CheckBox", "Spinner")):
        return False
    lowered = control_id.lower()
    return not any(token in lowered for token in UNSAFE_VIEW_TOKENS)


def generate(root: Path):
    strings = {}
    for path in (root / "app/src/main/res/values").glob("*.xml"):
        for node in ET.parse(path).getroot().findall("string"):
            strings[node.get("name")] = "".join(node.itertext()).strip()
    elements = [{"key": "dialog", "name": "对话框", "description": "不选择元素，以软件对话框显示文案。",
                 "page": "dialog", "autoClickAllowed": False}]
    for key, (name, page) in COMPOSE.items():
        elements.append({"key": key, "name": name, "description": name,
                         "page": page, "autoClickAllowed": key not in UNSAFE})
    settings = ("walk", "run", "bike", "random_offset", "lat_max_offset", "lon_max_offset",
                "log_off", "pos_history", "user_agreement", "disclaimer", "license_protocol",
                "reference_link", "project_link", "bilibili_link")
    for suffix in settings:
        name = strings["setting_" + suffix]
        elements.append({"key": "settings." + suffix, "name": name, "description": name,
                         "page": "settings", "autoClickAllowed": suffix in {
                             "random_offset", "log_off", "user_agreement", "disclaimer"}})
    auth = (("login.submit", "登录", "login"), ("login.register", "注册账号", "login"),
            ("login.logout", "退出登录", "login"), ("login.back", "登录页面返回", "login"),
            ("register.submit", "注册并登录", "register"), ("register.code", "发送验证码", "register"),
            ("register.back", "注册页面返回", "register"), ("settings.back", "设置页面返回", "settings"))
    for key, name, page in auth:
        elements.append({"key": key, "name": name, "description": name, "page": page,
                         "autoClickAllowed": key in {"login.register", "login.back", "register.back", "settings.back"}})
    for suffix, name in (("username", "账号"), ("password", "密码"), ("email", "邮箱"),
                         ("code", "验证码"), ("register_password", "密码（至少 8 位）"),
                         ("confirm_password", "确认密码")):
        page = "login" if suffix in {"username", "password"} else "register"
        elements.append({"key": page + ".input_" + suffix, "name": name, "description": name,
                         "page": page, "autoClickAllowed": False})
    layouts = {
        "activity_route": "routes", "activity_nfc_tools": "nfc",
        "activity_usage_tips": "tips", "activity_usage_tip_detail": "tip_detail",
        "activity_usage_tip_editor": "tip_editor", "activity_fsr_binding": "binding",
        "activity_history": "history",
        "route_local_item": "routes", "route_shared_item": "routes",
        "dialog_route_share": "draw", "dialog_nfc_share": "nfc",
        "dialog_nfc_manual_input": "simulation",
        "dialog_searchable_picker": "simulation",
        "history_item": "history",
    }
    for layout, page in layouts.items():
        for node in ET.parse(root / f"app/src/main/res/layout/{layout}.xml").iter():
            control_id = node.get(ANDROID + "id", "").split("/")[-1]
            if not control_id or not (node.tag.endswith(("Button", "EditText", "Switch", "CheckBox", "Spinner"))
                                      or node.get(ANDROID + "clickable") == "true"):
                continue
            text = next((node.get(ANDROID + prop) for prop in ("contentDescription", "hint", "queryHint", "text")
                         if node.get(ANDROID + prop)), "")
            name = strings.get(text.removeprefix("@string/"), text) if text else control_id
            if name == control_id:
                name = {"route_new_fab": "新建路线", "route_import_fab": "导入路线",
                        "tips_new_fab": "新建使用提示", "nfc_et_mock_url": "模拟 NFC 链接",
                        "nfc_et_mock_package": "模拟 NFC 包名", "tip_editor_title_input": "提示标题",
                        "tip_editor_contributor_input": "贡献者", "route_share_name_input": "分享路线名称",
                        "nfc_share_name_input": "分享 NFC 名称", "nfc_share_url_input": "分享 NFC 链接",
                        "nfc_share_package_input": "分享 NFC 包名", "nfc_manual_url_input": "NFC 链接",
                        "nfc_manual_package_input": "NFC 包名", "searchable_picker_input": "搜索选项",
                        "et_searchable_picker": "搜索选项"}.get(control_id, "控件：" + control_id)
            elements.append({"key": "view." + control_id, "name": name, "description": name,
                             "page": page, "autoClickAllowed": auto_click_allowed(node.tag, control_id)})
    simulation_labels = ("模拟方式", "速度", "步频", "实时路线编辑", "路线比例", "每米步数", "循环次数",
                         "网络模拟", "卫星数量", "信号质量：强", "信号质量：中", "信号质量：弱", "水平精度因子 HDOP",
                         "更新间隔", "速度浮动", "浮动范围", "浮动频率", "路径扰动", "路径幅度",
                         "海拔扰动", "基础海拔", "变化范围", "变化高度", "触发概率", "启用悬浮窗",
                         "缩放比例", "按钮大小", "选择提示音", "上传设置", "下载设置")
    for index, name in enumerate(simulation_labels):
        elements.append({"key": "simulation_settings.control_" + str(index), "name": name,
                         "description": name, "page": "simulation_settings", "autoClickAllowed": False})
    return list({element["key"]: element for element in elements}.values())


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--backend", type=Path, required=True)
    args = parser.parse_args()
    root = Path(__file__).resolve().parents[1]
    data = json.dumps(generate(root), ensure_ascii=False, indent=2) + "\n"
    for target in (root / "app/src/main/assets/tutorial_elements.json",
                   args.backend / "app/data/tutorial_elements.json"):
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(data, encoding="utf-8")
