import { Link } from 'react-router-dom';
import BrandLogo from '../components/BrandLogo';

const FEATURES = [
  {
    title: 'Fleet registry',
    description: 'Every vehicle, its details and status in one organized place — no more spreadsheets.',
    icon: (
      <svg aria-hidden="true" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
        <path d="M5 17h-2v-6l2-5h9l4 5h3v6h-2" />
        <circle cx="7.5" cy="17.5" r="2" />
        <circle cx="17.5" cy="17.5" r="2" />
      </svg>
    ),
  },
  {
    title: 'Smart document intake',
    description: 'Upload registrations, insurance and inspection papers — key data is extracted automatically for review.',
    icon: (
      <svg aria-hidden="true" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
        <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
        <path d="M14 2v6h6" />
        <path d="M9 13h6" />
        <path d="M9 17h4" />
      </svg>
    ),
  },
  {
    title: 'Expiry alerts',
    description: 'Know before an inspection or policy expires. Alerts keep your fleet compliant and on the road.',
    icon: (
      <svg aria-hidden="true" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
        <path d="M18 8a6 6 0 0 0-12 0c0 7-3 9-3 9h18s-3-2-3-9" />
        <path d="M13.7 21a2 2 0 0 1-3.4 0" />
      </svg>
    ),
  },
];

export default function LandingPage() {
  return (
    <div className="landing">
      <header className="landing-header">
        <BrandLogo className="landing-brand" />
        <Link to="/login" className="btn landing-signin">Sign In</Link>
      </header>

      <main>
        <section className="landing-hero">
          <h1>
            Your fleet&apos;s paperwork, <span>finally under control</span>
          </h1>
          <p>
            DoccuFleet keeps vehicle documents, deadlines and compliance in one place —
            so nothing expires unnoticed and your fleet stays on the road.
          </p>
          <div className="landing-cta">
            <Link to="/login" className="btn landing-cta-primary">Sign In</Link>
            <Link to="/register" className="btn btn-secondary">Create an account</Link>
          </div>
        </section>

        <section className="landing-features" aria-label="What DoccuFleet does">
          {FEATURES.map((feature) => (
            <article key={feature.title} className="landing-feature-card">
              <span className="landing-feature-icon">{feature.icon}</span>
              <h2>{feature.title}</h2>
              <p>{feature.description}</p>
            </article>
          ))}
        </section>
      </main>

      <footer className="landing-footer">
        <span>DoccuFleet — fleet document management</span>
        <Link to="/login">Sign In</Link>
      </footer>
    </div>
  );
}
