#!/bin/bash

# --- إعدادات المستخدم (استبدل القيم أدناه) ---
GITHUB_USERNAME="Nashwan2027"
REPO_NAME="Dubbing"
# مثال: https://github.com/username/repo.git
REPO_URL="https://github.com/$Nashwan2027/$Dubbing.git" 
# -------------------------------------------

echo "جاري تهيئة المشروع..."
git init

echo "جاري إضافة الملفات..."
git add .

echo "جاري عمل Commit أولي..."
git commit -m "Initial commit - Project upload"

echo "جاري ربط المستودع..."
git remote add origin $https://github.com/Nashwan2027/Dubbing.git

echo "جاري تغيير الفرع إلى main..."
git branch -M main

echo "جاري الرفع إلى GitHub..."
git push -u origin main

echo "تم الرفع بنجاح!"
