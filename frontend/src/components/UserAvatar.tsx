import { useEffect, useState } from 'react';
import { getProfileImage } from '../api/authApi';

interface UserAvatarProps {
  username: string | null | undefined;
  imageUrl?: string | null;
  className?: string;
}

function initialsFor(username: string | null | undefined) {
  return username ? username.slice(0, 2).toUpperCase() : '??';
}

export default function UserAvatar({ username, imageUrl, className = '' }: UserAvatarProps) {
  const [src, setSrc] = useState<string | null>(null);
  const [failed, setFailed] = useState(false);

  useEffect(() => {
    let objectUrl: string | null = null;
    let active = true;
    setSrc(null);
    setFailed(false);

    if (!imageUrl) {
      return () => undefined;
    }

    getProfileImage(imageUrl)
      .then((res) => {
        if (!active) {
          return;
        }
        objectUrl = URL.createObjectURL(res.data);
        setSrc(objectUrl);
      })
      .catch(() => {
        if (active) {
          setFailed(true);
        }
      });

    return () => {
      active = false;
      if (objectUrl) {
        URL.revokeObjectURL(objectUrl);
      }
    };
  }, [imageUrl]);

  const classes = `nav-avatar${className ? ` ${className}` : ''}`;

  if (src && !failed) {
    return <img className={classes} src={src} alt={username ? `${username} profile` : 'Profile'} />;
  }

  return (
    <div className={classes} aria-hidden="true" title={username ?? ''}>
      {initialsFor(username)}
    </div>
  );
}
