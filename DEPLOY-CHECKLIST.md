# 🚀 Quick Deploy Checklist

## ✅ Before You Start (5 min)

1. [ ] Create Supabase account → https://supabase.com
2. [ ] Create Render account → https://render.com  
3. [ ] Have GitHub repos ready (both frontend & backend)

---

## 📦 Deploy Order (30 minutes total)

### 1️⃣ Database First (5 min)
- [ ] Create Supabase project
- [ ] Save database password
- [ ] Copy connection URI

### 2️⃣ Backend Second (15 min)
- [ ] Commit deployment files to Git
- [ ] Create Render Web Service (Docker)
- [ ] Add environment variables
- [ ] Wait for build (~10 min)
- [ ] Copy backend URL

### 3️⃣ Frontend Last (10 min)
- [ ] Create `.env.production` with backend URL
- [ ] Commit to Git
- [ ] Create Render Static Site
- [ ] Wait for build (~5 min)
- [ ] Copy frontend URL

### 4️⃣ Final Config (2 min)
- [ ] Update backend CORS with frontend URL
- [ ] Test login

---

## 🎯 Environment Variables Needed

### Backend (Render):
```
SPRING_DATASOURCE_URL = postgresql://postgres:[PASSWORD]@[HOST]:5432/postgres
SPRING_DATASOURCE_USERNAME = postgres
SPRING_DATASOURCE_PASSWORD = [your-supabase-password]
SPRING_JPA_HIBERNATE_DDL_AUTO = update
JWT_SECRET = [run: openssl rand -base64 64]
JWT_EXPIRATION = 86400000
ALLOWED_ORIGINS = https://bjj-tournament-frontend.onrender.com
```

### Frontend (.env.production):
```
REACT_APP_API_URL=https://bjj-tournament-api.onrender.com/api
```

---

## ⏱️ Expected Wait Times

- **First backend deploy**: 10-15 minutes (Docker build)
- **Frontend deploy**: 5 minutes (npm build)
- **Backend cold start**: 30-60 seconds (first load after sleep)

---

## 💡 Pro Tips

1. **Generate JWT secret**: `openssl rand -base64 64`
2. **Test backend health**: `https://[your-backend].onrender.com/api/auth/me`
3. **Check logs**: Render Dashboard → Service → Logs tab
4. **Database issues**: Check Supabase Dashboard → Database → Connection pooling

---

## 🔴 If Something Fails

1. **Backend won't start** → Check env vars match Supabase exactly
2. **CORS errors** → Update ALLOWED_ORIGINS, redeploy backend
3. **Database errors** → Verify Supabase connection string format
4. **Build timeout** → Normal on first deploy, just wait

---

## 📱 After Deployment

**Your URLs:**
- Frontend: `https://bjj-tournament-frontend.onrender.com`
- Backend: `https://bjj-tournament-api.onrender.com`

**Bookmark these!** 🔖

First visit will take 60 seconds (backend waking up) - this is normal! ✅

---

**Total Cost: $0.00 forever** 🎉
