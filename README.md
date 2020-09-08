# MovementRecognizer
![ic_launcher_round](https://user-images.githubusercontent.com/61889565/92423972-d9ee7480-f137-11ea-9511-7e7b0b14fa6a.png)

## Summary
MovementRecognizer is an Android OS project built to recognize human movements with the use of a Microsft Bands' 2 wrist bands' accelerometer and gyroscope values.

## Configuration
As a pre-requisite, ensure your Microsoft Band 2 is configured for use on your android device.

Once the Microsoft Band 2 is bounded via bluetooth, the user needs to wear it using the position shown on the next image.
![bandPositionSmall](https://user-images.githubusercontent.com/61889565/92423971-d955de00-f137-11ea-970f-2356c3f950de.jpg)

## Movements to recognize
For now, two different movements are recognized by the app: "Up" and "Down" . Both examples are shown on the following image:
![movementsSmall](https://user-images.githubusercontent.com/61889565/92424924-8cbfd200-f13a-11ea-99e3-c8de8693f26e.png)

## Use
Once the apps Main Activity is launched, the user needs to wait until the bands communication status is set to 'Connected':
![MainScreenSmall](https://user-images.githubusercontent.com/61889565/92423976-da870b00-f137-11ea-8aff-13c9ec574c44.png)

Otherwise, an error message is shown:




![SamplingScreenSmall](https://user-images.githubusercontent.com/61889565/92423974-da870b00-f137-11ea-8bf5-4a9cc3a22101.png)
![PrecessingScreenSmall](https://user-images.githubusercontent.com/61889565/92423975-da870b00-f137-11ea-894c-bd011ef49eff.png)

![UpRecognizerSmall](https://user-images.githubusercontent.com/61889565/92423977-db1fa180-f137-11ea-8653-88ad23bc626a.png)
![DownRecognizerSmall](https://user-images.githubusercontent.com/61889565/92423980-db1fa180-f137-11ea-9240-6128fc20099e.png)

