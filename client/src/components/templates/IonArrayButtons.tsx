import React from 'react';
import { IonButton, IonIcon } from '@ionic/react';
import { arrowUp, arrowDown, copy, trash } from 'ionicons/icons';
import { IconButtonProps } from '@rjsf/utils';

export const IonMoveUpButton: React.FC<IconButtonProps> = (props) => {
  const { disabled, onClick } = props;
  const handleClick = (e: React.MouseEvent<HTMLIonButtonElement>) => {
    if (onClick) {
      onClick(e as unknown as React.MouseEvent<HTMLButtonElement>);
    }
  };

  return (
    <IonButton
      onClick={handleClick}
      disabled={disabled}
      size="small"
      fill="clear"
    >
      <IonIcon slot="icon-only" icon={arrowUp} />
    </IonButton>
  );
};

export const IonMoveDownButton: React.FC<IconButtonProps> = (props) => {
  const { disabled, onClick } = props;
  const handleClick = (e: React.MouseEvent<HTMLIonButtonElement>) => {
    if (onClick) {
      onClick(e as unknown as React.MouseEvent<HTMLButtonElement>);
    }
  };

  return (
    <IonButton
      onClick={handleClick}
      disabled={disabled}
      size="small"
      fill="clear"
    >
      <IonIcon slot="icon-only" icon={arrowDown} />
    </IonButton>
  );
};

export const IonCopyButton: React.FC<IconButtonProps> = (props) => {
  const { disabled, onClick } = props;
  const handleClick = (e: React.MouseEvent<HTMLIonButtonElement>) => {
    if (onClick) {
      onClick(e as unknown as React.MouseEvent<HTMLButtonElement>);
    }
  };

  return (
    <IonButton
      onClick={handleClick}
      disabled={disabled}
      size="small"
      fill="clear"
      color="primary"
    >
      <IonIcon slot="icon-only" icon={copy} />
    </IonButton>
  );
};

export const IonRemoveButton: React.FC<IconButtonProps> = (props) => {
  const { disabled, onClick } = props;
  const handleClick = (e: React.MouseEvent<HTMLIonButtonElement>) => {
    if (onClick) {
      onClick(e as unknown as React.MouseEvent<HTMLButtonElement>);
    }
  };

  return (
    <IonButton
      onClick={handleClick}
      disabled={disabled}
      size="small"
      fill="clear"
      color="danger"
    >
      <IonIcon slot="icon-only" icon={trash} />
    </IonButton>
  );
};
