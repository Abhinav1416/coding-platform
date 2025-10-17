// File Path: frontend/api/auth/google/callback.js
export default async function handler(req, res) {
  const { code } = req.query;

  if (!code) {
    return res.status(400).send('Error: No authorization code provided.');
  }

  // ✅ Use the relative path. Vercel's proxy will handle it.
  const backendUrl = `https://coding-platform-uyo1.vercel.app/api/login/oauth2/code/google`;

  try {
    // Make the request to the Vercel URL, which gets proxied to your backend
    const backendResponse = await fetch(`${backendUrl}?code=${code}`, {
      method: 'GET',
      redirect: 'manual' 
    });

    const locationHeader = backendResponse.headers.get('location');

    // If the backend sends a redirect, forward the user's browser to that new location
    if (backendResponse.status === 302 && locationHeader) {
      // The locationHeader from your backend should already contain the JWT token
      // We just need to construct the full URL for the client-side redirect
      const finalRedirectUrl = new URL(locationHeader, 'https://coding-platform-uyo1.vercel.app').toString();
      res.redirect(302, finalRedirectUrl);
    } else {
      // If something went wrong on the backend
      const errorText = await backendResponse.text();
      res.status(backendResponse.status).send(`Error communicating with backend: ${errorText}`);
    }
  } catch (error) {
    console.error('Error in Google callback handler:', error);
    res.status(500).send('Internal Server Error');
  }
}