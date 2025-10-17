// File Path: frontend/api/auth/google/callback.js
export default async function handler(req, res) {
  const { code } = req.query;
  if (!code) {
    return res.status(400).send('Error: No authorization code provided.');
  }
  const backendRedirectUri = 'http://code-duel-alb-2054648281.ap-south-1.elb.amazonaws.com/login/oauth2/code/google';
  try {
    const backendResponse = await fetch(`${backendRedirectUri}?code=${code}`, {
      method: 'GET',
      redirect: 'manual'
    });
    const locationHeader = backendResponse.headers.get('location');
    if (backendResponse.status === 302 && locationHeader) {
      const finalRedirectUrl = new URL(locationHeader, 'https://coding-platform-uyo1.vercel.app').toString();
      res.redirect(302, finalRedirectUrl);
    } else {
      const errorText = await backendResponse.text();
      res.status(500).send(`Error communicating with backend: ${errorText}`);
    }
  } catch (error) {
    res.status(500).send('Internal Server Error');
  }
}