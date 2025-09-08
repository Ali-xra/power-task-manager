#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import os
import shutil
from datetime import datetime
import sys

def create_backup():
    """
    اسکریپت بکاپ گیری از پروژه Android Studio
    """
    
    # تاریخ و ساعت فعلی
    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    
    # پوشه فعلی (جایی که اسکریپت اجرا می‌شود)
    current_dir = os.getcwd()
    project_name = os.path.basename(current_dir)
    
    # پوشه بکاپ
    backup_folder = f"backup_{project_name}_{timestamp}"
    backup_path = os.path.join(current_dir, backup_folder)
    
    # فایل‌ها و پوشه‌هایی که نباید کپی شوند
    ignore_items = {
        '.gradle',
        'build',
        '.idea',
        'local.properties',
        '*.iml',
        '__pycache__',
        '.git',
        'backup_*',  # پوشه‌های بکاپ قبلی
        '*.pyc',
        '.DS_Store'
    }
    
    # پوشه‌های مهم پروژه Android
    important_dirs = [
        'app/src/main/java',
        'app/src/main/res',
        'app/src/main/AndroidManifest.xml',
        'app/build.gradle.kts',
        'build.gradle.kts',
        'settings.gradle.kts',
        'gradle.properties'
    ]
    
    print(f"🔄 شروع بکاپ گیری از پروژه: {project_name}")
    print(f"📁 پوشه بکاپ: {backup_folder}")
    print("-" * 50)
    
    # ایجاد پوشه بکاپ
    os.makedirs(backup_path, exist_ok=True)
    
    # فایل برای ذخیره ساختار
    structure_file = os.path.join(backup_path, f"project_structure_{timestamp}.txt")
    
    # لیست فایل‌های کپی شده
    copied_files = []
    
    def should_ignore(path):
        """بررسی اینکه آیا فایل/پوشه باید نادیده گرفته شود"""
        for ignore in ignore_items:
            if ignore in path or path.endswith(ignore):
                return True
        return False
    
    def copy_with_structure(src_path, relative_path=""):
        """کپی فایل‌ها با حفظ ساختار و تغییر نام"""
        items = []
        
        for root, dirs, files in os.walk(src_path):
            # حذف پوشه‌های نادیده گرفته شده
            dirs[:] = [d for d in dirs if not should_ignore(d)]
            
            rel_dir = os.path.relpath(root, current_dir)
            
            if should_ignore(rel_dir):
                continue
            
            # کپی فایل‌ها
            for file in files:
                if should_ignore(file):
                    continue
                    
                src_file = os.path.join(root, file)
                rel_path = os.path.relpath(src_file, current_dir)
                
                # تبدیل مسیر به نام فایل (جایگزینی / با _)
                safe_filename = rel_path.replace(os.sep, '_').replace('/', '_')
                dest_file = os.path.join(backup_path, safe_filename)
                
                try:
                    shutil.copy2(src_file, dest_file)
                    copied_files.append((rel_path, safe_filename))
                    items.append(rel_path)
                    print(f"✅ کپی شد: {rel_path}")
                except Exception as e:
                    print(f"❌ خطا در کپی {rel_path}: {e}")
        
        return items
    
    def generate_tree_structure(path, prefix="", is_last=True):
        """ایجاد ساختار درختی از پروژه"""
        lines = []
        
        if os.path.isfile(path):
            return []
        
        items = []
        try:
            items = sorted(os.listdir(path))
            items = [item for item in items if not should_ignore(item)]
        except PermissionError:
            return []
        
        for i, item in enumerate(items):
            item_path = os.path.join(path, item)
            is_last_item = (i == len(items) - 1)
            
            # انتخاب نماد مناسب
            if is_last_item:
                connector = "└── "
                extension = "    "
            else:
                connector = "├── "
                extension = "│   "
            
            # اضافه کردن آیتم به ساختار
            if os.path.isdir(item_path):
                lines.append(f"{prefix}{connector}📁 {item}/")
                # بازگشتی برای زیرپوشه‌ها
                sub_lines = generate_tree_structure(
                    item_path, 
                    prefix + extension, 
                    is_last_item
                )
                lines.extend(sub_lines)
            else:
                # تعیین آیکون بر اساس نوع فایل
                if item.endswith('.kt'):
                    icon = "🎯"  # Kotlin
                elif item.endswith('.java'):
                    icon = "☕"  # Java
                elif item.endswith('.xml'):
                    icon = "📄"  # XML
                elif item.endswith('.gradle') or item.endswith('.kts'):
                    icon = "🔧"  # Gradle
                else:
                    icon = "📄"
                lines.append(f"{prefix}{connector}{icon} {item}")
        
        return lines
    
    # کپی کل پروژه
    print("\n📂 در حال کپی فایل‌ها...")
    all_files = copy_with_structure(current_dir)
    
    # ایجاد فایل ساختار
    print("\n📝 در حال ایجاد فایل ساختار پروژه...")
    with open(structure_file, 'w', encoding='utf-8') as f:
        f.write("=" * 60 + "\n")
        f.write(f"🚀 ساختار پروژه: {project_name}\n")
        f.write(f"📅 تاریخ بکاپ: {datetime.now().strftime('%Y/%m/%d %H:%M:%S')}\n")
        f.write(f"📁 تعداد فایل‌های کپی شده: {len(copied_files)}\n")
        f.write("=" * 60 + "\n\n")
        
        # ساختار درختی
        f.write("🌳 ساختار درختی پروژه:\n")
        f.write("-" * 40 + "\n")
        tree_lines = generate_tree_structure(current_dir)
        for line in tree_lines:
            f.write(line + "\n")
        
        # لیست فایل‌های کپی شده
        f.write("\n" + "=" * 60 + "\n")
        f.write("📋 لیست فایل‌های کپی شده:\n")
        f.write("-" * 40 + "\n")
        for original, backup in copied_files:
            f.write(f"✓ {original}\n")
            f.write(f"  → {backup}\n\n")
    
    # ایجاد فایل README
    readme_file = os.path.join(backup_path, "README.txt")
    with open(readme_file, 'w', encoding='utf-8') as f:
        f.write(f"""
=====================================
پروژه: {project_name}
تاریخ بکاپ: {datetime.now().strftime('%Y/%m/%d %H:%M:%S')}
=====================================

📱 توضیحات پروژه:
-----------------
این یک اپلیکیشن آلارم هوشمند برای اندروید است که دارای ویژگی‌های زیر می‌باشد:

✅ ویژگی‌های اصلی:
• زنگ صبحگاهی با چالش تایپ جملات انگیزشی
• یادآور شبانه برای ارزیابی روزانه (امتیاز 1 تا 10)
• مدیریت جملات انگیزشی دلخواه
• صفحه آمار و گزارش عملکرد

🔧 تکنولوژی‌ها:
• زبان: Kotlin
• IDE: Android Studio
• Min SDK: 21
• Target SDK: 34

📂 ساختار پروژه:
• app/src/main/java/com/Alixra/power/ - کدهای Kotlin
  - MainActivity.kt - صفحه اصلی
  - ui/AlarmActivity.kt - صفحه چالش آلارم
  - ui/EveningActivity.kt - صفحه ارزیابی شبانه
  - service/AlarmService.kt - سرویس پخش صدا
  - receiver/AlarmReceiver.kt - دریافت‌کننده آلارم
  - data/PreferencesManager.kt - مدیریت داده‌ها
  
• app/src/main/res/ - منابع
  - layout/ - فایل‌های XML رابط کاربری
  - drawable/ - تصاویر و پس‌زمینه‌ها
  - values/ - رنگ‌ها، استایل‌ها و رشته‌ها

📱 نحوه اجرا:
1. Android Studio را باز کنید
2. Open an Existing Project
3. پوشه پروژه را انتخاب کنید
4. Sync و Build کنید
5. روی دستگاه یا امولاتور اجرا کنید

⚠️ نکات مهم:
• حداقل Android 5.0 (API 21) مورد نیاز است
• مجوزهای لازم باید توسط کاربر تایید شوند
• برای آلارم دقیق، بهینه‌سازی باتری را غیرفعال کنید

=====================================
        """)
    
    print("\n" + "=" * 50)
    print(f"✅ بکاپ گیری با موفقیت انجام شد!")
    print(f"📁 محل ذخیره: {backup_path}")
    print(f"📊 تعداد فایل‌ها: {len(copied_files)}")
    print(f"📄 فایل ساختار: {os.path.basename(structure_file)}")
    print("=" * 50)

if __name__ == "__main__":
    try:
        create_backup()
        input("\n✨ برای خروج Enter را بزنید...")
    except KeyboardInterrupt:
        print("\n\n❌ عملیات لغو شد.")
        sys.exit(1)
    except Exception as e:
        print(f"\n❌ خطا: {e}")
        input("\nبرای خروج Enter را بزنید...")
        sys.exit(1)