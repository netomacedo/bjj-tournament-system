# BJJ Tournament System - Deployment Guide

## ✅ Docker Setup Complete

Your project now has complete Docker support:
- ✅ Backend Dockerfile (Spring Boot)
- ✅ Frontend Dockerfile (React + Nginx)
- ✅ docker-compose.yml (Full stack)

---

## 🆓 FREE Cloud Deployment Options

### **Option 1: Render.com (RECOMMENDED)**

**Why:** Best free tier, easiest setup, includes everything

**Free Tier:**
- ✅ Web Services (Backend + Frontend)
- ✅ PostgreSQL Database (90 days free, then $7/month)
- ✅ Auto-deploy from GitHub
- ✅ Free SSL certificates
- ⚠️ Services sleep after 15 min inactivity (takes 30s to wake up)

**Steps:**
1. Push code to GitHub
2. Go to [render.com](https://render.com)
3. Create account (free)
4. Click "New +" → Select:
   - **PostgreSQL** (create database first)
   - **Web Service** (for backend - point to backend folder)
   - **Static Site** (for frontend - point to frontend folder)
5. Set environment variables in Render dashboard
6. Deploy!

**Cost:** $0/month (or $7/month if you want database to persist after 90 days)

---

### **Option 2: Railway.app**

**Why:** Similar to Render, generous free tier

**Free Tier:**
- ✅ $5 worth of usage/month (usually enough for small apps)
- ✅ PostgreSQL included
- ✅ Auto-deploy from GitHub
- ⚠️ Credit card required (but won't charge until free credits exhausted)

**Steps:**
1. Push to GitHub
2. Go to [railway.app](https://railway.app)
3. "New Project" → "Deploy from GitHub"
4. Railway auto-detects and deploys both services
5. Add PostgreSQL from template
6. Done!

**Cost:** $0/month (with $5 free credits)

---

### **Option 3: Split Deployment (Best Performance)**

Deploy frontend and backend separately for better performance:

**Frontend:** Vercel or Netlify (BEST for React)
- ✅ 100% FREE forever
- ✅ Global CDN (super fast)
- ✅ Auto-deploy from GitHub
- ✅ Free SSL

**Backend + Database:** Render.com
- ✅ FREE (with sleep)
- ✅ PostgreSQL included

**Steps:**

#### Frontend (Vercel):
1. Push frontend to GitHub
2. Go to [vercel.com](https://vercel.com)
3. Import repository
4. Set environment variable:
   ```
   REACT_APP_API_URL=https://your-backend.onrender.com/api
   ```
5. Deploy! (URL: https://your-app.vercel.app)

#### Backend (Render):
1. Create Web Service on Render
2. Point to backend folder
3. Set Docker as environment
4. Add PostgreSQL database
5. Deploy!

**Cost:** $0/month

---

## 📊 Comparison Table

| Feature | Render | Railway | Vercel+Render |
|---------|--------|---------|---------------|
| **Cost** | $0-7/mo | $0/mo | $0/mo |
| **Setup Difficulty** | ⭐⭐ Easy | ⭐ Easiest | ⭐⭐⭐ Medium |
| **Performance** | Good | Good | ⭐⭐⭐ Best |
| **Sleep/Wake** | Yes | No* | Frontend: No, Backend: Yes |
| **Database** | Included | Included | Included |
| **Best For** | All-in-one | Quick start | Production apps |

*Railway has usage limits instead

---

## 🚀 Quick Start Commands

### Local Development with Docker:
```bash
# Start everything (database + backend + frontend)
cd bjj-tournament-system
docker-compose -f docker-compose.full.yml up --build

# Access:
# Frontend: http://localhost:80
# Backend: http://localhost:8080
# Database: localhost:5432
```

### Build Individual Services:
```bash
# Backend
cd bjj-tournament-system
docker build -t bjj-backend .

# Frontend
cd bjj-tournament-frontend
docker build -t bjj-frontend .
```

---

## 🎯 My Recommendation

**For testing (2x/year, 3 hours each):**
→ **Render.com** (Option 1)
- Simplest setup
- Everything in one place
- 30-second wake-up is fine for occasional use
- $0 cost (database sleeps when not used)

**For production:**
→ **Vercel (Frontend) + Render (Backend)** (Option 3)
- Best performance
- Frontend always fast (no sleep)
- Backend wakes quickly when needed
- $0 cost

---

## 📝 Environment Variables Needed

### Backend (Render/Railway):
```
SPRING_DATASOURCE_URL=jdbc:postgresql://<db-host>:5432/bjj_tournament
SPRING_DATASOURCE_USERNAME=<db-user>
SPRING_DATASOURCE_PASSWORD=<db-password>
SPRING_PROFILES_ACTIVE=prod
```

### Frontend (Vercel/Netlify):
```
REACT_APP_API_URL=https://your-backend-url.onrender.com/api
```

---

## 🔐 Security Notes

1. **Change JWT Secret** in production:
   ```
   jwt.secret=<generate-random-256-bit-key>
   ```

2. **Change Database Password**:
   - Don't use `bjj_password` in production
   - Use Render's auto-generated passwords

3. **Enable CORS** only for your frontend domain in SecurityConfig.java

---

## ✅ Ready to Deploy!

Your project is now fully containerized and ready for any cloud platform!

**Next steps:**
1. Choose your deployment option (I recommend Render)
2. Push code to GitHub
3. Follow the steps above
4. Share the URL! 🎉
