// File Path: frontend/api/auth/google/callback.js

export default async function handler(req, res) {
  // Get the authorization code from Google's redirect
  const { code } = req.query;

  if (!code) {
    return res.status(400).send('Error: No authorization code provided.');
  }

  // This is your real, insecure backend URL. Vercel calls this server-to-server.
  const backendRedirectUri = 'http://code-duel-alb-2054648281.ap-south-1.elb.amazonaws.com/login/oauth2/code/google';

  try {
    // Forward the code to your Spring Boot backend
    const backendResponse = await fetch(`${backendRedirectUri}?code=${code}`, {
      method: 'GET',
      redirect: 'manual' // We need to handle the redirect ourselves
    });

    // Your Spring Boot backend will respond with a redirect containing the tokens.
    // We extract this location from the response header.
    const locationHeader = backendResponse.headers.get('location');
    if (backendResponse.status === 302 && locationHeader) {

      // The location will be something like "/?accessToken=...&refreshToken=..."
      // We create the full final URL for the user's browser.
      const finalRedirectUrl = new URL(locationHeader, 'https://coding-platform-uyo1.vercel.app').toString();

      // Redirect the user's browser to the final destination with the tokens.
      res.redirect(302, finalRedirectUrl);

    } else {
      // If the backend didn't redirect, something went wrong.
      const errorText = await backendResponse.text();
      console.error('Backend error:', errorText);
      res.status(500).send(`Error communicating with backend: ${errorText}`);
    }
  } catch (error) {
    console.error('Internal server error:', error);
    res.status(500).send('Internal Server Error');
  }
}