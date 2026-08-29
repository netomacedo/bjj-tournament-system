# BJJ Tournament System - FREE Deployment Guide

## 🎯 100% FREE Deployment (Perfect for 2x/year use)

**Stack:**
- Frontend: Render Static Site (FREE forever, no sleep)
- Backend: Render Web Service (FREE forever, sleeps after 15min)
- Database: Supabase PostgreSQL (FREE forever, 500MB)

**Total Cost: $0.00/month**

---

## 📋 Prerequisites

1. GitHub account
2. Render account (https://render.com - free, no credit card)
3. Supabase account (https://supabase.com - free, no credit card)

---

## 🚀 Step-by-Step Deployment

### **Step 1: Setup Database (Supabase) - 5 minutes**

1. Go to https://supabase.com
2. Click **"Start your project"** → Sign in with GitHub
3. Click **"New Project"**
   - Name: `bjj-tournament-db`
   - Database Password: (create strong password - SAVE THIS!)
   - Region: Choose closest to you
   - Click **"Create new project"**

4. **Wait 2 minutes** for database to provision

5. Get connection details:
   - Go to **Settings** → **Database**
   - Find **"Connection string"** section
   - Copy the **"URI"** (looks like: `postgresql://postgres:[password]@[host]:5432/postgres`)
   - Replace `[password]` with your password
   - **SAVE THIS URL** - you'll need it!

6. Run initial schema (optional - can be done via Spring Boot auto-migration):
   - Go to **SQL Editor**
   - You can let Spring Boot create tables automatically, or run your schema manually

---

### **Step 2: Deploy Backend (Spring Boot) - 10 minutes**

1. **Push your code to GitHub** (if not already):
   ```bash
   cd /Users/macedo/workspace-development/bjj-tournament-system
   git add -A
   git commit -m "Add deployment configs"
   git push origin main
   ```

2. Go to https://render.com
3. Click **"New +"** → **"Web Service"**
4. Connect your GitHub repository: `bjj-tournament-system`
5. Configure:
   - **Name**: `bjj-tournament-api`
   - **Region**: Oregon (Free)
   - **Branch**: `main`
   - **Runtime**: Docker
   - **Plan**: Free

6. **Environment Variables** (click "Advanced"):
   ```
   SPRING_DATASOURCE_URL = [Your Supabase URI]
   SPRING_DATASOURCE_USERNAME = postgres
   SPRING_DATASOURCE_PASSWORD = [Your Supabase password]
   SPRING_JPA_HIBERNATE_DDL_AUTO = update
   JWT_SECRET = [Generate random 64-char string]
   JWT_EXPIRATION = 86400000
   ALLOWED_ORIGINS = https://bjj-tournament-frontend.onrender.com
   ```

   **To generate JWT_SECRET**, run in terminal:
   ```bash
   openssl rand -base64 64
   ```

7. Click **"Create Web Service"**
8. **Wait 10-15 minutes** for first build
9. Once deployed, copy your backend URL (e.g., `https://bjj-tournament-api.onrender.com`)

---

### **Step 3: Deploy Frontend (React) - 5 minutes**

1. **Update frontend API URL**:
   ```bash
   cd /Users/macedo/workspace-development/bjj-tournament-frontend
   ```

2. Create `.env.production`:
   ```bash
   cat > .env.production << 'ENVEOF'
   REACT_APP_API_URL=https://bjj-tournament-api.onrender.com/api
   ENVEOF
   ```

3. **Push to GitHub**:
   ```bash
   git add -A
   git commit -m "Add production environment config"
   git push origin main
   ```

4. Go to https://render.com
5. Click **"New +"** → **"Static Site"**
6. Connect your GitHub repository: `bjj-tournament-system-frontend`
7. Configure:
   - **Name**: `bjj-tournament-frontend`
   - **Branch**: `main`
   - **Build Command**: `npm install && npm run build`
   - **Publish Directory**: `build`

8. Click **"Create Static Site"**
9. **Wait 5 minutes** for build
10. Once deployed, copy your frontend URL (e.g., `https://bjj-tournament-frontend.onrender.com`)

---

### **Step 4: Update Backend CORS**

1. Go back to Render Dashboard → **bjj-tournament-api**
2. Go to **Environment** tab
3. Update `ALLOWED_ORIGINS` to your actual frontend URL
4. Click **"Save Changes"** (backend will redeploy)

---

## ✅ You're Done!

Your app is now live at:
- **Frontend**: `https://bjj-tournament-frontend.onrender.com`
- **Backend**: `https://bjj-tournament-api.onrender.com`
- **Database**: Supabase (managed)

---

## ⚡ Important Notes

### **Cold Starts (Expected)**
- Backend sleeps after 15 minutes of inactivity
- First request after sleep takes **30-60 seconds** to wake up
- **Perfect for 2x/year use!** - Just wait a minute on first load

### **How to use:**
1. Open frontend URL
2. **Wait 30-60 seconds** on first load (backend waking up)
3. Login and use normally
4. Done! Come back in 6 months 😊

---

## 🔧 Troubleshooting

### Backend won't start:
- Check Environment Variables are correct
- Check Supabase database is running
- Check logs in Render Dashboard

### Frontend 403/CORS errors:
- Update ALLOWED_ORIGINS in backend environment
- Make sure it matches your frontend URL exactly

### Database connection failed:
- Verify Supabase connection string
- Check password is correct
- Ensure database is not paused

---

## 💰 Cost Breakdown

| Service | Cost | Notes |
|---------|------|-------|
| Frontend (Render) | **$0** | Free forever |
| Backend (Render) | **$0** | Free forever (with sleep) |
| Database (Supabase) | **$0** | Free forever (500MB limit) |
| **TOTAL** | **$0/month** | 🎉 |

---

## 📊 Free Tier Limits

- **Supabase**: 500MB database, 1GB file storage
- **Render Backend**: 512MB RAM, sleeps after 15min
- **Render Frontend**: Unlimited bandwidth, 100GB/month builds

**These limits are MORE than enough for 2x/year use!** 🚀

---

## 🎓 Next Steps

1. Test your deployment
2. Create first tournament
3. Bookmark the URL
4. See you in 6 months! 😊

---

**Need help?** Check Render logs or Supabase dashboard for errors.

**Happy Rolling!** 🥋
