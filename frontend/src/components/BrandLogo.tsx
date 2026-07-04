interface BrandLogoProps {
  className?: string;
}

export default function BrandLogo({ className = '' }: BrandLogoProps) {
  return (
    <span className={`brand-logo${className ? ` ${className}` : ''}`}>
      <span className="brand-logo-image-frame" aria-hidden="true">
        <img src="/logo-doccufleet.jpg" alt="" />
      </span>
      <span className="brand-logo-text">DoccuFleet</span>
    </span>
  );
}
