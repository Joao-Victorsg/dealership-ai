export type CarCategory = "SUV" | "Sedan" | "Sport" | "Hatch" | "Pick-up";
export type CarType = "Electric" | "Combustion";
export type CarStatus = 1 | 2 | 3; // 1 Available, 2 Sold, 3 Unavailable

export interface Car {
  id: string;
  model: string;
  year: number;
  manufacturer: string;
  externalColor: string;
  internalColor: string;
  optionalItems: string[];
  kilometers: number;
  type: CarType;
  category: CarCategory;
  isNew: boolean;
  value: number;
  status: CarStatus;
  registrationDate: string;
  imageQuery: string; // hint for placeholder image
}

export const CARS: Car[] = [
  {
    id: "tesla-model-3-2024",
    model: "Model 3",
    year: 2024,
    manufacturer: "Tesla",
    externalColor: "Pearl White",
    internalColor: "Black",
    optionalItems: ["Autopilot", "Premium audio", "Glass roof"],
    kilometers: 0,
    type: "Electric",
    category: "Sedan",
    isNew: true,
    value: 289000,
    status: 1,
    registrationDate: "2025-09-12",
    imageQuery: "white tesla model 3 sedan",
  },
  {
    id: "honda-civic-2023",
    model: "Civic Touring",
    year: 2023,
    manufacturer: "Honda",
    externalColor: "Lunar Silver",
    internalColor: "Beige",
    optionalItems: ["Sunroof", "Leather seats", "GPS", "Adaptive cruise"],
    kilometers: 18400,
    type: "Combustion",
    category: "Sedan",
    isNew: false,
    value: 178500,
    status: 1,
    registrationDate: "2025-08-02",
    imageQuery: "silver honda civic touring sedan",
  },
  {
    id: "toyota-hilux-2024",
    model: "Hilux SRX",
    year: 2024,
    manufacturer: "Toyota",
    externalColor: "Granite Grey",
    internalColor: "Black",
    optionalItems: ["4x4", "Tow package", "Bedliner"],
    kilometers: 12000,
    type: "Combustion",
    category: "Pick-up",
    isNew: false,
    value: 312000,
    status: 1,
    registrationDate: "2025-07-21",
    imageQuery: "grey toyota hilux pickup truck",
  },
  {
    id: "porsche-911-2022",
    model: "911 Carrera",
    year: 2022,
    manufacturer: "Porsche",
    externalColor: "Guards Red",
    internalColor: "Black",
    optionalItems: ["Sport Chrono", "PASM", "Bose audio"],
    kilometers: 22000,
    type: "Combustion",
    category: "Sport",
    isNew: false,
    value: 845000,
    status: 1,
    registrationDate: "2025-06-15",
    imageQuery: "red porsche 911 carrera",
  },
  {
    id: "vw-golf-gti-2024",
    model: "Golf GTI",
    year: 2024,
    manufacturer: "Volkswagen",
    externalColor: "Tornado Red",
    internalColor: "Tartan",
    optionalItems: ["DCC", "Harman Kardon", "LED Matrix"],
    kilometers: 0,
    type: "Combustion",
    category: "Hatch",
    isNew: true,
    value: 245000,
    status: 1,
    registrationDate: "2025-09-30",
    imageQuery: "red volkswagen golf gti hatch",
  },
  {
    id: "byd-yuan-plus-2024",
    model: "Yuan Plus",
    year: 2024,
    manufacturer: "BYD",
    externalColor: "Atlantic Blue",
    internalColor: "White",
    optionalItems: ["360° camera", "Heat pump", "Vehicle-to-load"],
    kilometers: 0,
    type: "Electric",
    category: "SUV",
    isNew: true,
    value: 199900,
    status: 1,
    registrationDate: "2025-10-05",
    imageQuery: "blue byd yuan plus electric suv",
  },
  {
    id: "jeep-compass-2023",
    model: "Compass Limited",
    year: 2023,
    manufacturer: "Jeep",
    externalColor: "Carbon Black",
    internalColor: "Brown leather",
    optionalItems: ["Panoramic roof", "Adaptive cruise", "Beats audio"],
    kilometers: 27800,
    type: "Combustion",
    category: "SUV",
    isNew: false,
    value: 184000,
    status: 1,
    registrationDate: "2025-05-11",
    imageQuery: "black jeep compass suv",
  },
  {
    id: "ford-ranger-2024",
    model: "Ranger Raptor",
    year: 2024,
    manufacturer: "Ford",
    externalColor: "Code Orange",
    internalColor: "Black",
    optionalItems: ["Fox shocks", "Terrain management", "Bedliner"],
    kilometers: 5400,
    type: "Combustion",
    category: "Pick-up",
    isNew: false,
    value: 459000,
    status: 1,
    registrationDate: "2025-09-01",
    imageQuery: "orange ford ranger raptor pickup",
  },
  {
    id: "audi-rs5-2023",
    model: "RS5 Sportback",
    year: 2023,
    manufacturer: "Audi",
    externalColor: "Nardo Grey",
    internalColor: "Red leather",
    optionalItems: ["Carbon trim", "B&O audio", "Ceramic brakes"],
    kilometers: 14200,
    type: "Combustion",
    category: "Sport",
    isNew: false,
    value: 612000,
    status: 1,
    registrationDate: "2025-04-18",
    imageQuery: "grey audi rs5 sportback",
  },
  {
    id: "fiat-pulse-2024",
    model: "Pulse Impetus",
    year: 2024,
    manufacturer: "Fiat",
    externalColor: "Strada White",
    internalColor: "Black",
    optionalItems: ["CVT", "Wireless charger"],
    kilometers: 0,
    type: "Combustion",
    category: "SUV",
    isNew: true,
    value: 124900,
    status: 1,
    registrationDate: "2025-10-12",
    imageQuery: "white fiat pulse compact suv",
  },
  {
    id: "renault-zoe-2022",
    model: "Zoe E-Tech",
    year: 2022,
    manufacturer: "Renault",
    externalColor: "Ocean Blue",
    internalColor: "Grey",
    optionalItems: ["Heat pump", "Reverse camera"],
    kilometers: 31200,
    type: "Electric",
    category: "Hatch",
    isNew: false,
    value: 132000,
    status: 1,
    registrationDate: "2025-03-22",
    imageQuery: "blue renault zoe electric hatchback",
  },
  {
    id: "bmw-x5-2024",
    model: "X5 xDrive40i",
    year: 2024,
    manufacturer: "BMW",
    externalColor: "Mineral White",
    internalColor: "Cognac leather",
    optionalItems: ["M Sport package", "Harman Kardon", "Laser headlights"],
    kilometers: 0,
    type: "Combustion",
    category: "SUV",
    isNew: true,
    value: 698000,
    status: 1,
    registrationDate: "2025-09-25",
    imageQuery: "white bmw x5 luxury suv",
  },
];

export const SOLD_CARS: Car[] = [
  { ...CARS[1], id: "sold-civic-1", status: 2, value: 178500 },
];

export interface MockUser {
  email: string;
  firstName: string;
  lastName: string;
  cpf: string;
  phone: string;
  postCode: string;
  streetNumber: string;
  resolvedStreet: string;
  resolvedNeighborhood: string;
  resolvedCity: string;
  resolvedState: string;
}

export const DEFAULT_USER: MockUser = {
  email: "ana.silva@example.com",
  firstName: "Ana",
  lastName: "Silva",
  cpf: "12345678901",
  phone: "11987654321",
  postCode: "01310100",
  streetNumber: "1578",
  resolvedStreet: "Avenida Paulista",
  resolvedNeighborhood: "Bela Vista",
  resolvedCity: "São Paulo",
  resolvedState: "SP",
};

export interface MockPurchase {
  id: string;
  car: Car;
  saleValue: number; // includes tax
  date: string;
}

export const PURCHASES: MockPurchase[] = [
  {
    id: "p-1001",
    car: CARS[5],
    saleValue: 215892,
    date: "2025-08-14",
  },
  {
    id: "p-1002",
    car: CARS[10],
    saleValue: 142560,
    date: "2025-04-02",
  },
];

/** Mocked CEP → address lookup */
export function resolveCep(cep: string) {
  const clean = cep.replace(/\D/g, "");
  if (clean.length !== 8) return null;
  // Simple deterministic mock so the UI can show resolved data
  return {
    street: clean.startsWith("01") ? "Avenida Paulista" : "Rua das Acácias",
    neighborhood: clean.startsWith("01") ? "Bela Vista" : "Centro",
    city: clean.startsWith("01") ? "São Paulo" : "Rio de Janeiro",
    state: clean.startsWith("01") ? "SP" : "RJ",
  };
}

export const findCar = (id: string) => CARS.find((c) => c.id === id);
